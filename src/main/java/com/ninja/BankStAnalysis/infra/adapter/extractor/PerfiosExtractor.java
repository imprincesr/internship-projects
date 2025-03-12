package com.ninja.BankStAnalysis.infra.adapter.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PerfiosExtractor implements BankStatementExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(Provider provider) {
        return provider == Provider.PERFIOS;
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



    private ObjectNode transformJson(String jsonString) throws Exception {
        List<String> accountNos = JsonPath.read(jsonString, "$.accountXns[*].accountNo");

        ArrayNode customerDetailsArray = extractCustomerDetails(jsonString);
        ArrayNode accountXnsArray = extractAccountTransactions(jsonString, accountNos);
        ArrayNode eodArray = extractEodBalances(jsonString, accountNos);
        ArrayNode bankTransactionsArray = extractBankTransactions(jsonString, accountNos);


        ObjectNode finalJson = objectMapper.createObjectNode();
        finalJson.set("customerDetails", customerDetailsArray);
        finalJson.set("accountXns", accountXnsArray);
        finalJson.set("eod", eodArray);
        finalJson.set("bankTransactions", bankTransactionsArray);

        return finalJson;
    }

    private ArrayNode extractCustomerDetails(String jsonString) {

        ArrayNode customerDetailsArray = objectMapper.createArrayNode();

        int totalAccountCount = JsonPath.read(jsonString, "$.accountXns.length()");

        for (int i = 0; i < totalAccountCount; i++) {

            ObjectNode customerDetails = objectMapper.createObjectNode();
            customerDetails.put("name", safeReadJsonPath(jsonString, "$.customerInfo.name"));
            customerDetails.put("mobileNo", safeReadJsonPath(jsonString, "$.customerInfo.mobile"));
            customerDetails.put("pan", safeReadJsonPath(jsonString, "$.customerInfo.pan"));
            customerDetails.put("aadharMasked", safeReadJsonPath(jsonString, "$.customerInfo.aadharMasked"));
            customerDetails.put("bankName", safeReadJsonPath(jsonString, "$.customerInfo.bank"));

            customerDetailsArray.add(customerDetails);
        }

        return customerDetailsArray;
    }

    private ArrayNode extractAccountTransactions(String jsonString, List<String> accountNos) {
        List<Map<String, Object>> accountXnsList = JsonPath.read(jsonString, "$.accountXns");
        ArrayNode accountXnsArray = objectMapper.createArrayNode();

        for (int i = 0; i < accountXnsList.size(); i++) {
            Map<String, Object> accountXns = accountXnsList.get(i);
            String accountNo = (String) accountXns.get("accountNo");
            String accountType = (String) accountXns.get("accountType");
            List<Map<String, Object>> xns = (List<Map<String, Object>>) accountXns.get("xns");

            ArrayNode xnsArray = objectMapper.createArrayNode();
            for (int j = 0; j < xns.size(); j++) {
                Map<String, Object> txn = xns.get(j);
                ObjectNode transaction = objectMapper.createObjectNode();
                transaction.put("date", (String) txn.get("date"));
                transaction.put("amount", ((Number) txn.get("amount")).doubleValue());
                transaction.put("balance", ((Number) txn.get("balance")).doubleValue());
                transaction.put("narration", (String) txn.get("narration"));
                xnsArray.add(transaction);
            }

            ObjectNode accountXnsObject = objectMapper.createObjectNode();
            accountXnsObject.put("accountNo", accountNo);
            accountXnsObject.put("accountType", accountType);
            accountXnsObject.set("xns", xnsArray);
            accountXnsArray.add(accountXnsObject);
        }

        return accountXnsArray;
    }

    private ArrayNode extractEodBalances(String jsonString, List<String> accountNos) {
        List<Map<String, Object>> accountAnalysisList = JsonPath.read(jsonString, "$.accountAnalysis");
        ArrayNode eodArray = objectMapper.createArrayNode();

        for (int i = 0; i < accountAnalysisList.size(); i++) {
            Map<String, Object> accountAnalysis = accountAnalysisList.get(i);
            String accountNo = accountNos.get(i);
            List<Map<String, Object>> eodBalances = (List<Map<String, Object>>) accountAnalysis.get("eODBalances");

            ArrayNode balanceArray = objectMapper.createArrayNode();
            for (int j = 0; j < eodBalances.size(); j++) {
                Map<String, Object> eod = eodBalances.get(j);
                ObjectNode balanceObject = objectMapper.createObjectNode();
                balanceObject.put("date", (String) eod.get("date"));
                balanceObject.put("balance", ((Number) eod.get("balance")).doubleValue());
                balanceArray.add(balanceObject);
            }

            ObjectNode eodObject = objectMapper.createObjectNode();
            eodObject.put("accountNo", accountNo);
            eodObject.set("balances", balanceArray);
            eodArray.add(eodObject);
        }

        return eodArray;
    }

    private ArrayNode extractBankTransactions(String jsonString, List<String> accountNos) {
        List<Map<String, Object>> accountXnsList = JsonPath.read(jsonString, "$.accountXns");
        ArrayNode bankTransactionsArray = objectMapper.createArrayNode();

        for (int i = 0; i < accountXnsList.size(); i++) {
            Map<String, Object> accountXns = accountXnsList.get(i);
            String accountNo = (String) accountXns.get("accountNo");
            List<Map<String, Object>> xns = (List<Map<String, Object>>) accountXns.get("xns");

            ArrayNode transactionArray = objectMapper.createArrayNode();
            for (int j = 0; j < xns.size(); j++) {
                Map<String, Object> txn = xns.get(j);
                ObjectNode transaction = objectMapper.createObjectNode();
                transaction.put("date", (String) txn.get("date"));
                transaction.put("amount", ((Number) txn.get("amount")).doubleValue());
                transaction.put("balance", ((Number) txn.get("balance")).doubleValue());
                transactionArray.add(transaction);
            }

            ObjectNode bankTransactionObject = objectMapper.createObjectNode();
            bankTransactionObject.put("accountNo", accountNo);
            bankTransactionObject.set("transactions", transactionArray);
            bankTransactionsArray.add(bankTransactionObject);
        }

        return bankTransactionsArray;
    }

    // Helper method to convert Number list to Double list
    private List<Double> convertToDoubleList(List<Number> numbers) {
        List<Double> doubles = new ArrayList<>();
        for (Number num : numbers) {
            doubles.add(num.doubleValue()); // Converts Integer, Long, or Double to Double
        }
        return doubles;
    }

    private String safeReadJsonPath(String json, String path) {
        try {
            return JsonPath.read(json, path);
        } catch (Exception e) {
            return null;
        }
    }
}

