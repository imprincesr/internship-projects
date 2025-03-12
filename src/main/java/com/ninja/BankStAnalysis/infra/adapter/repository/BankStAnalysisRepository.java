package com.ninja.BankStAnalysis.infra.adapter.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.ninja.BankStAnalysis.core.modelHelper.BankStatementHashType;
import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import com.ninja.BankStAnalysis.core.modelHelper.SourceType;
import com.ninja.BankStAnalysis.core.port.out.BankStAnalysisRepositoryPort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BankStAnalysisRepository implements BankStAnalysisRepositoryPort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<Map<String, Object>> saveDetails(Integer userId, String realmId, Provider provider, String report, String response) {
        validateInputs(userId, realmId, provider, report, response);
        try {
            List<Object[]> statementBatch = new ArrayList<>();
            List<List<Object[]>> transactionBatches = new ArrayList<>();

            // Process each JSON path and collect statements and their transactions
            processIfPathExists(userId, realmId, provider, report, response, statementBatch, transactionBatches, "$.accountXns[*]");
            processIfPathExists(userId, realmId, provider, report, response, statementBatch, transactionBatches, "$.eod[*]");
            processIfPathExists(userId, realmId, provider, report, response, statementBatch, transactionBatches, "$.bankTransactions[*]");

            List<Map<String, Object>> savedStatements = new ArrayList<>();
            List<Long> statementIds = new ArrayList<>();

            if (!statementBatch.isEmpty()) {
                savedStatements = insertUserBankStatement(statementBatch);
                // Extract IDs from saved statements for transaction linking
                statementIds = savedStatements.stream()
                        .map(row -> (Long) row.get("id"))
                        .toList();
                log.info("Inserted {} bank statements for userId: {}", statementBatch.size(), userId);
            } else {
                log.warn("No bank statements to insert for userId: {}", userId);
            }

            if (!transactionBatches.isEmpty() && !statementIds.isEmpty()) {
                List<Object[]> allTransactions = new ArrayList<>();
                for (int i = 0; i < transactionBatches.size(); i++) {
                    List<Object[]> transactions = transactionBatches.get(i);
                    for (Object[] transaction : transactions) {
                        transaction[transaction.length - 1] = statementIds.get(i); // Assign correct statement_id
                        allTransactions.add(transaction);
                    }
                }
                insertUserBankTransaction(allTransactions);
                log.info("Inserted {} transactions for userId: {}", allTransactions.size(), userId);
            } else {
                log.debug("No transactions to insert for userId: {}", userId);
            }

            return savedStatements;

        } catch (Exception e) {
            log.error("Failed to save bank statement details for userId: {}, realmId: {}, provider: {}", userId, realmId, provider, e);
            throw new RuntimeException("Error parsing JSON or saving data: " + e.getMessage(), e);
        }
    }

    private void validateInputs(Integer userId, String realmId, Provider provider, String report, String response) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(realmId, "realmId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(report, "report must not be null");
        Objects.requireNonNull(response, "response must not be null");
    }

    private void processIfPathExists(Integer userId, String realmId, Provider provider, String report, String response,
                                     List<Object[]> statementBatch, List<List<Object[]>> transactionBatches, String jsonPath) {
        try {
            List<Map<String, Object>> accounts = JsonPath.read(response, jsonPath);
            if (accounts == null || accounts.isEmpty()) {
                log.debug("No accounts found for JSON path: {} in response for userId: {}", jsonPath, userId);
                return;
            }
            processAccounts(userId, realmId, provider, report, accounts, statementBatch, transactionBatches, jsonPath);
        } catch (PathNotFoundException e) {
            log.info("JSON path not found: {} for userId: {}, skipping this section", jsonPath, userId);
        } catch (Exception e) {
            log.error("Unexpected error processing JSON path: {} for userId: {}", jsonPath, userId, e);
        }
    }

    private void processAccounts(Integer userId, String realmId, Provider provider, String report, List<Map<String, Object>> accounts,
                                 List<Object[]> statementBatch, List<List<Object[]>> transactionBatches, String jsonPath) {
        try {
            for (int i = 0; i < accounts.size(); i++) {
                Map<String, Object> account = accounts.get(i);
                String accountNumber = (String) account.get("accountNo");
                String rootHash = (String) account.get("token");

                if (accountNumber == null || rootHash == null) {
                    log.warn("Skipping account at index {} for userId: {} due to missing accountNumber or rootHash", i, userId);
                    continue;
                }

                int hashType = determineHashType(jsonPath);
                int providerType = provider.ordinal();
                int sourceType = SourceType.BATCH.ordinal();
                String mediaLink = "";
                String phoneNumber = safeJsonPathRead(report, String.format("$.customerDetails[%d].mobileNo", i), String.class).orElse("");
                String createdBy = "SYSTEM";

                statementBatch.add(new Object[]{
                        userId, realmId, accountNumber, phoneNumber, rootHash, hashType,
                        providerType, sourceType, mediaLink, createdBy, new Timestamp(System.currentTimeMillis())
                });

                List<Object[]> transactionBatch = new ArrayList<>();
                processTransactions(userId, realmId, accountNumber, account, transactionBatch, hashType, providerType);
                transactionBatches.add(transactionBatch);
            }
        } catch (Exception e) {
            log.error("Error processing accounts for userId: {}", userId, e);
        }
    }

    private void processTransactions(Integer userId, String realmId, String accountNumber, Map<String, Object> account,
                                     List<Object[]> transactionBatch, int hashType, int providerType) {
        List<Map<String, Object>> transactions = extractTransactions(account);
        if (transactions == null || transactions.isEmpty()) {
            log.debug("No transactions found for account: {} under userId: {}", accountNumber, userId);
            return;
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (Map<String, Object> txn : transactions) {
            String txnHash = (String) txn.get("token");
            if (txnHash != null) {
                transactionBatch.add(new Object[]{
                        userId, realmId, accountNumber, txnHash, hashType, providerType, now, null
                });
            }
        }
    }

    private List<Map<String, Object>> extractTransactions(Map<String, Object> account) {
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) account.get("xns");
        if (transactions == null) {
            transactions = (List<Map<String, Object>>) account.get("balances");
        }
        if (transactions == null) {
            transactions = (List<Map<String, Object>>) account.get("transactions");
        }
        return transactions;
    }

    public static <T> Optional<T> safeJsonPathRead(String json, String path, Class<T> type) {
        try {
            Object result = JsonPath.read(json, path);
            if (result != null && type.isInstance(result)) {
                return Optional.of(type.cast(result));
            }
            log.debug("No valid data found at JSON path: {}", path);
        } catch (Exception e) {
            log.warn("Failed to read JSON path '{}': {}", path, e.getMessage());
        }
        return Optional.empty();
    }

    private int determineHashType(String jsonPath) {
        if (jsonPath.contains("accountXns")) return BankStatementHashType.ACCOUNT_XNS.ordinal();
        if (jsonPath.contains("eod")) return BankStatementHashType.EOD_BALANCE.ordinal();
        if (jsonPath.contains("bankTransactions")) return BankStatementHashType.BANK_TRANSACTION.ordinal();
        return BankStatementHashType.ACCOUNT_XNS.ordinal();
    }

    private List<Map<String, Object>> insertUserBankStatement(List<Object[]> statementBatch) {
        if (statementBatch.isEmpty()) return Collections.emptyList();

        StringBuilder sql = new StringBuilder("""
            INSERT INTO user_bank_statement
            (user_id, realm_id, account_number, phone_number, root_hash, hash_type, provider, source_type, media_link, created_by, created_at)
            VALUES
        """);
        for (int i = 0; i < statementBatch.size(); i++) {
            sql.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            if (i < statementBatch.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(" RETURNING *;");

        List<Map<String, Object>> savedStatements = jdbcTemplate.query(sql.toString(), ps -> {
            int paramIndex = 1;
            for (Object[] args : statementBatch) {
                ps.setInt(paramIndex++, (Integer) args[0]);         // user_id
                ps.setString(paramIndex++, (String) args[1]);       // realm_id
                ps.setString(paramIndex++, (String) args[2]);       // account_number
                ps.setString(paramIndex++, (String) args[3]);       // phone_number
                ps.setString(paramIndex++, (String) args[4]);       // root_hash
                ps.setInt(paramIndex++, (Integer) args[5]);         // hash_type
                ps.setInt(paramIndex++, (Integer) args[6]);         // provider
                ps.setInt(paramIndex++, (Integer) args[7]);         // source_type
                ps.setString(paramIndex++, (String) args[8]);       // media_link
                ps.setString(paramIndex++, (String) args[9]);       // created_by
                ps.setTimestamp(paramIndex++, (Timestamp) args[10]); // created_at
            }
        }, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("user_id", rs.getInt("user_id"));
            row.put("realm_id", rs.getString("realm_id"));
            row.put("account_number", rs.getString("account_number"));
            row.put("phone_number", rs.getString("phone_number"));
            row.put("root_hash", rs.getString("root_hash"));
            row.put("hash_type", rs.getInt("hash_type"));
            row.put("provider", rs.getInt("provider"));
            row.put("source_type", rs.getInt("source_type"));
            row.put("media_link", rs.getString("media_link"));
            row.put("created_by", rs.getString("created_by"));
            row.put("created_at", rs.getTimestamp("created_at"));
            return row;
        });

        if (savedStatements.size() != statementBatch.size()) {
            log.error("Mismatch in saved statements: expected {}, got {}", statementBatch.size(), savedStatements.size());
            throw new RuntimeException("Failed to retrieve all saved statements");
        }

        return savedStatements;
    }

    private void insertUserBankTransaction(List<Object[]> transactionBatch) {
        String insertTransactionSQL = """
            INSERT INTO user_bank_transaction
            (user_id, realm_id, account_number, hash, hash_type, provider, created_at, statement_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.batchUpdate(insertTransactionSQL, transactionBatch);
    }
}