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

@Slf4j
@Component
public class OneMoneyExtractor implements BankStatementExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(Provider provider) {
        return provider == Provider.ONEMONEY;
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

        JsonNode accountsNode = rootNode.path("data");
        for (JsonNode account : accountsNode) {
            customerDetailsArray.add(createCustomerDetails(account));
            accountXnsArray.add(createAccountXns(account));
//            eodArray.add(createEod(account));
            bankTransactionsArray.add(createBankTransactions(account));
        }

        outputNode.set("customerDetails", customerDetailsArray);
        outputNode.set("accountXns", accountXnsArray);
//        outputNode.set("eod", eodArray);
        outputNode.set("bankTransactions", bankTransactionsArray);

        return outputNode;

    }

    // Method to create customerDetails section
    private ObjectNode createCustomerDetails(JsonNode account) {
        String name = account.path("Profile").path("Holders").path("Holder").path("name").asText();
        String mobileNo = account.path("Profile").path("Holders").path("Holder").path("mobile").asText();
        String pan = account.path("Profile").path("Holders").path("Holder").path("pan").asText();
        String aadharMasked = account.path("Profile").path("Holders").path("Holder").path("aadharMasked").asText();
        String bankName = account.path("fipName").asText();

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
        String accountNo = account.path("maskedAccNumber").asText();
        String accountType = account.path("Summary").path("type").asText();

        ObjectNode accountXnNode = objectMapper.createObjectNode();
        accountXnNode.put("accountNo", accountNo);
        accountXnNode.put("accountType", accountType);

        ArrayNode xnsArray = objectMapper.createArrayNode();
        JsonNode transactions = account.path("Transactions").path("Transaction");
        for (JsonNode txn : transactions) {
            ObjectNode xnNode = objectMapper.createObjectNode();
            String date = txn.path("valueDate").asText();
            double amount = txn.path("amount").asDouble();
            String txnType = txn.path("type").asText();
            xnNode.put("date", date);
            xnNode.put("amount", "DEBIT".equals(txnType) ? -amount:amount);
            xnNode.put("balance", txn.path("currentBalance").asDouble());
            xnNode.put("narration", txn.path("narration").asText());
            xnsArray.add(xnNode);
        }
        accountXnNode.set("xns", xnsArray);
        return accountXnNode;
    }

    // Method to create eod section
//    private ObjectNode createEod(JsonNode account) {
//        String accountNo = account.path("data").path("account_details").path("account_number").asText();
//
//        ObjectNode eodNode = objectMapper.createObjectNode();
//        eodNode.put("accountNo", accountNo);
//
//        ArrayNode balancesArray = objectMapper.createArrayNode();
//        JsonNode eodBalances = account.path("data").path("eod_balances");
//        for (Iterator<String> it = eodBalances.fieldNames(); it.hasNext(); ) {
//            String monthYear = it.next();
//            if ("Months_order".equals(monthYear) || "start_date".equals(monthYear)) continue;
//
//            String[] parts = monthYear.split("-");
//            String monthName = parts[0];
//            int year = Integer.parseInt("20" + parts[1]);
//            int monthNum = getMonthNumber(monthName);
//            int daysInMonth = getLastDayOfMonth(monthName, year);
//
//            JsonNode dailyBalances = eodBalances.path(monthYear);
//            for (int day = 0; day < dailyBalances.size() && day < daysInMonth; day++) {
//                double balance = dailyBalances.get(day).asDouble();
//                String fullDate = String.format("%d-%02d-%02d", year, monthNum, day + 1);
//                ObjectNode balanceNode = objectMapper.createObjectNode();
//                balanceNode.put("date", fullDate);
//                balanceNode.put("balance", balance);
//                balancesArray.add(balanceNode);
//            }
//        }
//        eodNode.set("balances", balancesArray);
//        return eodNode;
//    }

    // Method to create bankTransactions section
    private ObjectNode createBankTransactions(JsonNode account) {
        String accountNo = account.path("maskedAccNumber").asText();

        ObjectNode bankTxnNode = objectMapper.createObjectNode();
        bankTxnNode.put("accountNo", accountNo);

        ArrayNode bankTxnsArray = objectMapper.createArrayNode();
        JsonNode transactions = account.path("Transactions").path("Transaction");
        for (JsonNode txn : transactions) {
            ObjectNode bankTxn = objectMapper.createObjectNode();
            String date = txn.path("valueDate").asText();
            double amount = txn.path("amount").asDouble();
            String txnType = txn.path("type").asText();
            bankTxn.put("date", date);
            bankTxn.put("amount", "DEBIT".equals(txnType) ? -amount:amount);
            bankTxn.put("balance", txn.path("currentBalance").asDouble());
            bankTxnsArray.add(bankTxn);
        }
        bankTxnNode.set("transactions", bankTxnsArray);
        return bankTxnNode;
    }
}