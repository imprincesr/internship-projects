package com.ninja.BankStAnalysis.infra.adapter.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Iterator;

@Slf4j
@Component
public class FinboxExtractor implements BankStatementExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(Provider provider) {
        return provider == Provider.FINBOX;
    }

    @Override
    public String processStatement(MultipartFile bankStatement) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(bankStatement.getInputStream());
            String jsonString = jsonNode.toString();

            ObjectNode finalJson = transformJson(jsonString);

            return objectMapper.writeValueAsString(finalJson);

        } catch (Exception e) {
            log.error("Error processing bank statement: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process bank statement", e);
        }
    }

    // Main transformation method
    private ObjectNode transformJson(String inputJson) throws IOException {
        JsonNode rootNode = objectMapper.readTree(inputJson);
        ObjectNode outputNode = objectMapper.createObjectNode();

        ArrayNode customerDetailsArray = objectMapper.createArrayNode();
        ArrayNode accountXnsArray = objectMapper.createArrayNode();
        ArrayNode eodArray = objectMapper.createArrayNode();
        ArrayNode bankTransactionsArray = objectMapper.createArrayNode();

        JsonNode accountsNode = rootNode.path("accounts");
        for (JsonNode account : accountsNode) {
            customerDetailsArray.add(createCustomerDetails(account));
            accountXnsArray.add(createAccountXns(account));
            eodArray.add(createEod(account));
            bankTransactionsArray.add(createBankTransactions(account));
        }

        outputNode.set("customerDetails", customerDetailsArray);
        outputNode.set("accountXns", accountXnsArray);
        outputNode.set("eod", eodArray);
        outputNode.set("bankTransactions", bankTransactionsArray);

        return outputNode;

    }

    // Method to create customerDetails section
    private ObjectNode createCustomerDetails(JsonNode account) {
        String name = account.path("data").path("account_details").path("name").asText();
        String mobileNo = account.path("data").path("account_details").path("phone_number").asText();
        String pan = account.path("data").path("account_details").path("pan_number").asText();
        String aadharMasked = account.path("data").path("account_details").path("aadhar_masked").asText();
        String bankName = account.path("data").path("account_details").path("bank").asText();

        ObjectNode customerDetailsNode = objectMapper.createObjectNode();
        customerDetailsNode.put("name", name);
        customerDetailsNode.put("mobileNo", mobileNo);
        customerDetailsNode.put("pan", pan);
        customerDetailsNode.put("aadharMasked", aadharMasked);
        customerDetailsNode.put("bankName", bankName);

        return customerDetailsNode;
    }

    // Method to create accountXns section
    private ObjectNode createAccountXns(JsonNode account) {
        String accountNo = account.path("data").path("account_details").path("account_number").asText();
        String accountType = account.path("data").path("account_details").path("account_category").asText();

        ObjectNode accountXnNode = objectMapper.createObjectNode();
        accountXnNode.put("accountNo", accountNo);
        accountXnNode.put("accountType", accountType);

        ArrayNode xnsArray = objectMapper.createArrayNode();
        JsonNode transactions = account.path("data").path("transactions");
        for (JsonNode txn : transactions) {
            ObjectNode xnNode = objectMapper.createObjectNode();
            String date = txn.path("date").asText().substring(0, 10);
            double amount = txn.path("amount").asDouble();
            String txnType = txn.path("transaction_type").asText();
            xnNode.put("date", date);
            xnNode.put("amount", "debit".equals(txnType) ? -amount:amount);
            xnNode.put("balance", txn.path("balance").asDouble());
            xnNode.put("narration", txn.path("transaction_note").asText());
            xnsArray.add(xnNode);
        }
        accountXnNode.set("xns", xnsArray);
        return accountXnNode;
    }

    // Method to create eod section
    private ObjectNode createEod(JsonNode account) {
        String accountNo = account.path("data").path("account_details").path("account_number").asText();

        ObjectNode eodNode = objectMapper.createObjectNode();
        eodNode.put("accountNo", accountNo);

        ArrayNode balancesArray = objectMapper.createArrayNode();
        JsonNode eodBalances = account.path("data").path("eod_balances");
        for (Iterator<String> it = eodBalances.fieldNames(); it.hasNext(); ) {
            String monthYear = it.next();
            if ("Months_order".equals(monthYear) || "start_date".equals(monthYear)) continue;

            String[] parts = monthYear.split("-");
            String monthName = parts[0];
            int year = Integer.parseInt("20" + parts[1]);
            int monthNum = getMonthNumber(monthName);
            int daysInMonth = getLastDayOfMonth(monthName, year);

            JsonNode dailyBalances = eodBalances.path(monthYear);
            for (int day = 0; day < dailyBalances.size() && day < daysInMonth; day++) {
                double balance = dailyBalances.get(day).asDouble();
                String fullDate = String.format("%d-%02d-%02d", year, monthNum, day + 1);
                ObjectNode balanceNode = objectMapper.createObjectNode();
                balanceNode.put("date", fullDate);
                balanceNode.put("balance", balance);
                balancesArray.add(balanceNode);
            }
        }
        eodNode.set("balances", balancesArray);
        return eodNode;
    }

    // Method to create bankTransactions section
    private ObjectNode createBankTransactions(JsonNode account) {
        String accountNo = account.path("data").path("account_details").path("account_number").asText();

        ObjectNode bankTxnNode = objectMapper.createObjectNode();
        bankTxnNode.put("accountNo", accountNo);

        ArrayNode bankTxnsArray = objectMapper.createArrayNode();
        JsonNode transactions = account.path("data").path("transactions");
        for (JsonNode txn : transactions) {
            ObjectNode bankTxn = objectMapper.createObjectNode();
            String date = txn.path("date").asText().substring(0, 10);
            double amount = txn.path("amount").asDouble();
            String txnType = txn.path("transaction_type").asText();
            bankTxn.put("date", date);
            bankTxn.put("amount", "debit".equals(txnType) ? -amount:amount);
            bankTxn.put("balance", txn.path("balance").asDouble());
            bankTxnsArray.add(bankTxn);
        }
        bankTxnNode.set("transactions", bankTxnsArray);
        return bankTxnNode;
    }

    // Helper method to convert month abbreviation to number
    private int getMonthNumber(String monthName) {
        switch (monthName) {
            case "Jan":
                return 1;
            case "Feb":
                return 2;
            case "Mar":
                return 3;
            case "Apr":
                return 4;
            case "May":
                return 5;
            case "Jun":
                return 6;
            case "Jul":
                return 7;
            case "Aug":
                return 8;
            case "Sep":
                return 9;
            case "Oct":
                return 10;
            case "Nov":
                return 11;
            case "Dec":
                return 12;
            default:
                return -1; // Invalid month
        }
    }

    // Helper method to get the last day of the month, accounting for leap years
    private int getLastDayOfMonth(String monthName, int year) {
        int month = getMonthNumber(monthName);
        if (month==-1) return 31; // Default to 31 for invalid month

        if (month==2) { // February
            return (year % 4==0 && year % 100!=0) || (year % 400==0) ? 29:28;
        } else if (month==4 || month==6 || month==9 || month==11) { // 30-day months
            return 30;
        } else { // 31-day months
            return 31;
        }
    }
}