package com.ninja.BankStAnalysis.infra.adapter.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class ScoremeExtractor implements BankStatementExtractor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Provider provider) {
        return provider == Provider.SCOREME;
    }

    @Override
    public String processStatement(MultipartFile bankStatement) throws Exception {
        try (InputStream inputStream = bankStatement.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            ObjectNode finalJson = transformExcel(workbook);

            return objectMapper.writeValueAsString(finalJson);

        } catch (Exception e) {
            log.error("Error processing bank statement: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process bank statement", e);
        }
    }

    private ObjectNode transformExcel(Workbook workbook) throws Exception {

        ObjectNode outputNode = objectMapper.createObjectNode();

        ArrayNode customerDetailsArray = objectMapper.createArrayNode();
        ArrayNode accountXnsArray = objectMapper.createArrayNode();
        ArrayNode eodArray = objectMapper.createArrayNode();
        ArrayNode bankTransactionsArray = objectMapper.createArrayNode();

        customerDetailsArray.add(createCustomerDetails(workbook));
        accountXnsArray.add(createAccountXns(workbook));
        eodArray.add(createEod(workbook));
        bankTransactionsArray.add(createBankTransactions(workbook));

        outputNode.set("customerDetails", customerDetailsArray);
        outputNode.set("accountXns", accountXnsArray);
        outputNode.set("eod", eodArray);
        outputNode.set("bankTransactions", bankTransactionsArray);

        return outputNode;

    }

    private ObjectNode createCustomerDetails(Workbook workbook) {

        ObjectNode customerDetails = objectMapper.createObjectNode();
        Sheet sheet = workbook.getSheetAt(0);

        // Extract Name from merged cells (Row 7, Columns E-H)
        String name = getMergedCellValue(sheet, 6, 4, 7);
        name = name.replace("Bank Statement Analysis Report - ", "").trim();


        String mobileNo = "";
        String pan = "";
        String aadharMasked = "";
        String bankName = getCellData(sheet, 8, 6); // Row 9, Column G

        customerDetails.put("name", name);
        customerDetails.put("mobileNo", mobileNo);
        customerDetails.put("pan", pan);
        customerDetails.put("aadharMasked", aadharMasked);
        customerDetails.put("bankName", bankName);

        return customerDetails;
    }

    private String getMergedCellValue(Sheet sheet, int rowIndex, int startCol, int endCol) {
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.getFirstRow() == rowIndex &&
                    region.getFirstColumn() >= startCol &&
                    region.getLastColumn() <= endCol) {

                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Cell cell = row.getCell(region.getFirstColumn());
                    return getCellValueAsString(cell);
                }
            }
        }
        return "";
    }

    private ObjectNode createAccountXns(Workbook workbook) throws Exception {

        Sheet sheet = workbook.getSheetAt(1);

        List<String> sheetDates = readColumnData(sheet, 2);
        List<String> sheetDebitAmount = readColumnData(sheet, 5);
        List<String> sheetCreditAmount = readColumnData(sheet, 6);
        List<String> sheetBalances = readColumnData(sheet, 7);
        List<String> sheetNarration = readColumnData(sheet, 3);

        List<String> dates = convertDateFormat(sheetDates);
        List<Double> balances = convertBalanceFormat(sheetBalances);
        List<Double> amounts = mergeDebitCredit(sheetDebitAmount, sheetCreditAmount);


        // Construct the new JSON output using Jackson
        ArrayNode xnsArray = objectMapper.createArrayNode();
        int maxSize = Math.max(dates.size(), Math.max(amounts.size(), balances.size()));

        for (int i = 0; i < maxSize; i++) {
            ObjectNode transactionObject = objectMapper.createObjectNode();

            if (i < dates.size() && dates.get(i)!=null) {
                transactionObject.put("date", dates.get(i));
            }
            if (i < amounts.size() && amounts.get(i)!=null) {
                transactionObject.put("amount", amounts.get(i));
            }
            if (i < balances.size() && balances.get(i)!=null) {
                transactionObject.put("balance", balances.get(i));
            }
            if (i < sheetNarration.size() && sheetNarration.get(i)!=null) {
                transactionObject.put("narration", sheetNarration.get(i));
            }
            xnsArray.add(transactionObject);
        }

        // Extract account details
        String accountNo = getCellData(workbook.getSheetAt(0), 9, 6);
        String accountType = getCellData(workbook.getSheetAt(0), 12, 6);

        // Create account JSON object
        ObjectNode accountXnsObject = objectMapper.createObjectNode();
        accountXnsObject.put("accountNo", accountNo);
        accountXnsObject.put("accountType", accountType);
        accountXnsObject.set("xns", xnsArray);

        return accountXnsObject;

    }

    private ObjectNode createBankTransactions(Workbook workbook) throws Exception {

        Sheet sheet = workbook.getSheetAt(1);

        List<String> sheetDates = readColumnData(sheet, 2);
        List<String> sheetDebitAmount = readColumnData(sheet, 5);
        List<String> sheetCreditAmount = readColumnData(sheet, 6);
        List<String> sheetBalances = readColumnData(sheet, 7);

        List<String> dates = convertDateFormat(sheetDates);
        List<Double> balances = convertBalanceFormat(sheetBalances);
        List<Double> amounts = mergeDebitCredit(sheetDebitAmount, sheetCreditAmount);

        // Construct the new JSON output using Jackson
        ArrayNode transactionsArray = objectMapper.createArrayNode();
        int maxSize = Math.max(dates.size(), Math.max(amounts.size(), balances.size()));

        for (int i = 0; i < maxSize; i++) {
            ObjectNode transactionObject = objectMapper.createObjectNode();

            if (i < dates.size() && dates.get(i)!=null) {
                transactionObject.put("date", dates.get(i));
            }
            if (i < amounts.size() && amounts.get(i)!=null) {
                transactionObject.put("amount", amounts.get(i));
            }
            if (i < balances.size() && balances.get(i)!=null) {
                transactionObject.put("balance", balances.get(i));
            }
            transactionsArray.add(transactionObject);
        }

        // Extract account details
        String accountNo = getCellData(workbook.getSheetAt(0), 9, 6);

        // Create account JSON object
        ObjectNode bankTransactionsObject = objectMapper.createObjectNode();
        bankTransactionsObject.put("accountNo", accountNo);
        bankTransactionsObject.set("transactions", transactionsArray);

        return bankTransactionsObject;

    }

    private ObjectNode createEod(Workbook workbook) throws Exception{

        Sheet sheet = workbook.getSheetAt(9);

        List<Map<String, String>> data = readDataFromExcelSheetEOD(sheet);


        String accountNo = getCellData(workbook.getSheetAt(0), 9, 6);

        ArrayNode balances = objectMapper.createArrayNode();

        for (Map<String, String> entry : data) {
            String monthYear = entry.get("Month/Year");
            if (monthYear == null || !monthYear.contains(" ")) continue; // Ensure valid format

            String[] monthYearParts = monthYear.split("\\s+");
            if (monthYearParts.length < 2) continue; // Avoid index errors

            String year = monthYearParts[1];
            String month = monthYearParts[0];

            // Add balance for 1st day
            addBalanceEntry(balances, year, month, 1, entry.get("1"));

            // Add balances for 5th, 10th, 15th, 20th, 25th days
            for (int day = 5; day <= 25; day += 5) {
                addBalanceEntry(balances, year, month, day, entry.get(Integer.toString(day)));
            }

            // Add last day balance
            int lastDay = getLastDayOfMonth(month, Integer.parseInt(year));
            addBalanceEntry(balances, year, month, lastDay, entry.get("Last Day"));
        }

        ObjectNode eodbalancesObject = objectMapper.createObjectNode();
        eodbalancesObject.put("accountNo", accountNo);
        eodbalancesObject.set("balances", balances);

        return eodbalancesObject;
    }

    public List<Map<String, String>> readDataFromExcelSheetEOD(Sheet sheet) throws Exception {
        List<Map<String, String>> data = new ArrayList<>();

        int totalRows = sheet.getLastRowNum(); // Count of actual rows with data

        if (totalRows <= 7) return data; // Ensure valid data range

        Row headerRow = sheet.getRow(5); // Get header row for column names
        if (headerRow==null) return data; // Prevent null pointer errors

        for (int rowIndex = 7; rowIndex < totalRows; rowIndex++) { // Start from row 8 (index 7)
            Row row = sheet.getRow(rowIndex);
            if (row==null) continue;

            Map<String, String> entry = new HashMap<>();
            Cell monthYearCell = row.getCell(1);
            if (monthYearCell==null) continue; // Skip if Month/Year is missing

            String monthYear = getCellValueAsString(monthYearCell);
            entry.put("Month/Year", monthYear);

            for (int colIndex = 9; colIndex <= 15; colIndex++) { // Columns J to P (index 9 to 15)
                Cell headerCell = headerRow.getCell(colIndex);
                Cell cell = row.getCell(colIndex);

                if (headerCell!=null) {
                    String key = getCellValueAsString(headerCell);
                    String value = getCellValueAsString(cell);
                    entry.put(key, value);
                }
            }
            data.add(entry);
        }
        return data;
    }

    private List<String> readColumnData(Sheet sheet, int columnIndex) {
        List<String> dataList = new ArrayList<>();
        int rowCount = sheet.getPhysicalNumberOfRows();
        for (int i = 5; i < rowCount - 1; i++) {  // Start from row index 6
            Row row = sheet.getRow(i);
            if (row!=null) {
                Cell cell = row.getCell(columnIndex);
                if (cell!=null) {
                    dataList.add(cell.toString()); // Only add non-empty values
                }
            }
        }
        return dataList;
    }

    private List<String> convertDateFormat(List<String> dateList) {
        List<String> formattedDates = new ArrayList<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MMM-yyyy"); // Input format
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd"); // Desired format

        for (String dateStr : dateList) {
            try {
                Date date = inputFormat.parse(dateStr);
                formattedDates.add(outputFormat.format(date));
            } catch (ParseException e) {
                formattedDates.add(dateStr); // Keep original value if parsing fails
            }
        }
        return formattedDates;
    }

    private List<Double> convertBalanceFormat(List<String> balances) {
        List<Double> convertedBalances = new ArrayList<>();

        for (String balance : balances) {
            if (balance!=null && !balance.trim().isEmpty()) {
                try {
                    String sanitizedBalance = balance.replaceAll(",", ""); // Remove commas
                    double parsedBalance = Double.parseDouble(sanitizedBalance);
                    convertedBalances.add(Math.abs(parsedBalance)); // Ensure absolute value
                } catch (NumberFormatException e) {
                    convertedBalances.add(0.0);
//                    log.error("Invalid number format: " + balance);
                }
            }
        }
        return convertedBalances;
    }

    private List<Double> mergeDebitCredit(List<String> debitList, List<String> creditList) {
        List<Double> mergedList = new ArrayList<>();
        int size = Math.min(debitList.size(), creditList.size()); // Ensure same length

        for (int i = 0; i < size; i++) {
            String debit = debitList.get(i).trim();
            String credit = creditList.get(i).trim();

            if (!credit.equals("-") && !credit.isEmpty()) {
                mergedList.add(Double.parseDouble(credit)); // Add credit as positive
            } else if (!debit.equals("-") && !debit.isEmpty()) {
                mergedList.add(-Double.parseDouble(debit)); // Add debit as negative
            }
        }
        return mergedList;
    }

    private String getCellData(Sheet sheet, int rowNum, int colNum) {
        try {
            Row row = sheet.getRow(rowNum);
            if (row==null) return ""; // Return empty if row is missing

            Cell cell = row.getCell(colNum);
            if (cell==null) return ""; // Return empty if cell is missing

            return getCellValueAsString(cell);
        } catch (Exception e) {
            throw new RuntimeException("Error reading cell at row " + rowNum + ", col " + colNum, e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString(); // Convert date to string
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return String.valueOf(cell.getCellFormula());
            case BLANK:
                return "";
            default:
                return "Unsupported Cell Type";
        }
    }

    private void addBalanceEntry(ArrayNode balances, String year, String month, int day, String balanceStr) {
        if (balanceStr != null && !balanceStr.trim().isEmpty()) {
            ObjectNode balanceEntry = objectMapper.createObjectNode();
            balanceEntry.put("date", String.format("%s-%02d-%02d", year, getMonthNumber(month), day));
            balanceEntry.put("balance", parseBalance(balanceStr));
            balances.add(balanceEntry);
        }
    }

    private double parseBalance(String balanceStr) {
        if (balanceStr == null) return 0.0;
        balanceStr = balanceStr.replaceAll("[^0-9.]", ""); // Keep only numbers and decimal points
        try {
            return Double.parseDouble(balanceStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int getMonthNumber(String monthName) {
        switch (monthName) {
            case "Jan": return 1;
            case "Feb": return 2;
            case "Mar": return 3;
            case "Apr": return 4;
            case "May": return 5;
            case "Jun": return 6;
            case "Jul": return 7;
            case "Aug": return 8;
            case "Sep": return 9;
            case "Oct": return 10;
            case "Nov": return 11;
            case "Dec": return 12;
            default: return -1;
        }
    }

    private int getLastDayOfMonth(String monthName, int year) {
        int month = getMonthNumber(monthName);
        if (month==-1) return 31;

        if (month==2) {
            return (year % 4==0 && year % 100!=0) || year % 400==0 ? 29:28;
        } else if (month==4 || month==6 || month==9 || month==11) {
            return 30;
        } else {
            return 31;
        }
    }

}
