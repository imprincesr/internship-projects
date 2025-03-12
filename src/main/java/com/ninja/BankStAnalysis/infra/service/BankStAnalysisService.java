package com.ninja.BankStAnalysis.infra.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.JsonPath;
import com.ninja.BankStAnalysis.core.ananomyzer.impl.Anonymizer;
import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import com.ninja.BankStAnalysis.core.port.in.BankStAnalysisServicePort;
import com.ninja.BankStAnalysis.core.port.out.BankStAnalysisRepositoryPort;
import com.ninja.BankStAnalysis.core.port.out.BankStatementExtractionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class BankStAnalysisService implements BankStAnalysisServicePort {

    private final BankStatementExtractionPort extractionPort;
    private final BankStAnalysisRepositoryPort bankStAnalysisRepositoryPort;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> processBankStatement(Integer userId, String realmId, MultipartFile bankStatement){

        validateInput(userId, realmId, bankStatement);

        Provider provider = identifyProvider(bankStatement)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider for file: " + bankStatement.getOriginalFilename()));

        log.info("Processing bank statement for User: {}, Realm: {}, Provider: {}", userId, realmId, provider);

        try {
            String report = extractionPort.extractReport(provider, bankStatement);
            if (report==null) {
                log.error("Failed to extract report for Provider: {}", provider);
                throw new RuntimeException("Report extraction failed.");
            }

            Map<String, Object> merkleSummary = generateTokens(userId, realmId, report);
            String jsonMerkleSummary = objectMapper.writeValueAsString(merkleSummary);

            if (provider!=Provider.SCOREME) {
                JsonNode jsonNode = objectMapper.readTree(bankStatement.getInputStream());
                String bankStatementJson = jsonNode.toString();
            }


            log.info("Bank statement processed successfully for User: {}", userId);
            return persistData(userId, realmId, provider, report, jsonMerkleSummary);

        } catch (Exception e) {
            log.error("Failed to process bank statement for userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error processing bank statement", e);
        }
    }

    private void validateInput(Integer userId, String realmId, MultipartFile bankStatement) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(realmId, "realmId must not be null");
        if (bankStatement==null || bankStatement.isEmpty()) {
            throw new IllegalArgumentException("Bank Statement file cannot be null or empty");
        }
    }

    private Optional<Provider> identifyProvider(MultipartFile bankStatement) {
        String fileType = identifyFileType(bankStatement);

        return switch (fileType) {
            case "JSON" -> identifyJsonProvider(bankStatement);
            case "EXCEL" -> identifyExcelProvider(bankStatement);
            default -> Optional.empty();
        };
    }

    private String identifyFileType(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        if (contentType!=null) {
            if ("application/json".equalsIgnoreCase(contentType)) return "JSON";
            if ("application/vnd.ms-excel".equalsIgnoreCase(contentType) ||
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equalsIgnoreCase(contentType)) {
                return "EXCEL";
            }
        }

        if (filename!=null) {
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.endsWith(".json")) return "JSON";
            if (lowerFilename.endsWith(".xls") || lowerFilename.endsWith(".xlsx")) return "EXCEL";
        }

        log.warn("Unknown file type for file: {}", filename);
        return "UNKNOWN";
    }

    private Optional<Provider> identifyJsonProvider(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            JsonNode rootNode = objectMapper.readTree(inputStream);
            if (rootNode.has("accountXns") ||
                    (rootNode.has("report") && rootNode.get("report").has("accountXns"))) {
                return Optional.of(Provider.PERFIOS);
            }
            if (rootNode.has("accounts")) {
                return Optional.of(Provider.FINBOX);
            }
            if (rootNode.has("data")) {
                return Optional.of(Provider.ONEMONEY);
            }
            return Optional.empty();
        } catch (IOException e) {
            log.warn("Failed to identify JSON provider for file: {}, error: {}", file.getOriginalFilename(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Provider> identifyExcelProvider(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet==null) return Optional.empty();

//            String firstCellData = getCellData(sheet, 0, 0).toLowerCase();
            String firstCellData = "scoreme";
            if (firstCellData.contains("scoreme")) return Optional.of(Provider.SCOREME);

            return Optional.empty();
        } catch (IOException e) {
            log.warn("Failed to identify Excel provider for file: {}, error: {}", file.getOriginalFilename(), e.getMessage());
            return Optional.empty();
        }
    }

    private static String getCellData(Sheet sheet, int rowNum, int colNum) {
        try {
            Row row = sheet.getRow(rowNum);
            if (row==null) return "";

            Cell cell = row.getCell(colNum);
            if (cell==null) return "";

            return getCellValueAsString(cell);
        } catch (Exception e) {
            throw new RuntimeException("Error reading cell at row " + rowNum + ", col " + colNum + "for the purpose of identifying Excel Provider", e);
        }
    }

    private static String getCellValueAsString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC ->
                    DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue().toString():String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK -> "";
            default -> "";
        };
    }

    private Map<String, Object> generateTokens(Integer userId, String realmId, String jsonInput) {

        Anonymizer anonymizer = new Anonymizer(userId, realmId, true);
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Parse input JSON
            JsonNode rootNode = mapper.readTree(jsonInput);
            Map<String, Object> response = new LinkedHashMap<>();

            // Process accountXns
            if (rootNode.has("accountXns")) {
                ArrayNode accountXns = (ArrayNode) rootNode.get("accountXns");
                List<Map<String, Object>> processedAccounts = new ArrayList<>();

                for (JsonNode account : accountXns) {
                    Map<String, Object> processedAccount = new LinkedHashMap<>();
                    processedAccount.put("token", anonymizer.generateToken(account.toString()));
                    processedAccount.put("accountNo", account.get("accountNo").asText());
                    processedAccount.put("accountType", account.get("accountType").asText());

                    List<Map<String, Object>> processedXns = new ArrayList<>();
                    for (JsonNode xn : account.get("xns")) {
                        Map<String, Object> processedXn = mapper.convertValue(xn, Map.class);
                        processedXn.put("token", anonymizer.generateToken(xn.toString()));
                        processedXns.add(processedXn);
                    }
                    processedAccount.put("xns", processedXns);
                    processedAccounts.add(processedAccount);
                }
                response.put("accountXns", processedAccounts);
            }

            // Process bankTransactions
            if (rootNode.has("bankTransactions")) {
                ArrayNode bankTransactions = (ArrayNode) rootNode.get("bankTransactions");
                List<Map<String, Object>> processedTransactions = new ArrayList<>();

                for (JsonNode transaction : bankTransactions) {
                    Map<String, Object> processedTransaction = new LinkedHashMap<>();
                    processedTransaction.put("token", anonymizer.generateToken(transaction.toString()));
                    processedTransaction.put("accountNo", transaction.get("accountNo").asText());

                    List<Map<String, Object>> processedTxns = new ArrayList<>();
                    for (JsonNode txn : transaction.get("transactions")) {
                        Map<String, Object> processedTxn = mapper.convertValue(txn, Map.class);
                        processedTxn.put("token", anonymizer.generateToken(txn.toString()));
                        processedTxns.add(processedTxn);
                    }
                    processedTransaction.put("transactions", processedTxns);
                    processedTransactions.add(processedTransaction);
                }
                response.put("bankTransactions", processedTransactions);
            }

            // Process eod
            if (rootNode.has("eod")) {
                ArrayNode eod = (ArrayNode) rootNode.get("eod");
                List<Map<String, Object>> processedEod = new ArrayList<>();

                for (JsonNode eodEntry : eod) {
                    Map<String, Object> processedEntry = new LinkedHashMap<>();
                    processedEntry.put("token", anonymizer.generateToken(eodEntry.toString()));
                    processedEntry.put("accountNo", eodEntry.get("accountNo").asText());

                    List<Map<String, Object>> processedBalances = new ArrayList<>();
                    for (JsonNode balance : eodEntry.get("balances")) {
                        Map<String, Object> processedBalance = mapper.convertValue(balance, Map.class);
                        processedBalance.put("token", anonymizer.generateToken(balance.toString()));
                        processedBalances.add(processedBalance);
                    }
                    processedEntry.put("balances", processedBalances);
                    processedEod.add(processedEntry);
                }
                response.put("eod", processedEod);
            }

            return response;

        } catch (Exception e) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    private List<Map<String, Object>> persistData(Integer userId, String realmId, Provider provider, String report, String summaryJson) {
        log.info("Data persisted successfully for User: {}, Provider: {}", userId, provider);
        return bankStAnalysisRepositoryPort.saveDetails(userId, realmId, provider, report, summaryJson);
    }


}





