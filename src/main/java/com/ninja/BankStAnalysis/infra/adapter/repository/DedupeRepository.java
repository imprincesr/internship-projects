package com.ninja.BankStAnalysis.infra.adapter.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.ninja.BankStAnalysis.core.modelHelper.BankStatementHashType;
import com.ninja.BankStAnalysis.core.modelHelper.DedupeStatus;
import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import com.ninja.BankStAnalysis.core.port.out.DedupeRepositoryPort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DedupeRepository implements DedupeRepositoryPort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Map<String, Object> fetchDetails(Integer userId, String realmId, Provider provider, String report, String response) {
        log.info("Fetching dedupe details for userId: {}, realmId: {}, provider: {}", userId, realmId, provider);

        validateInputs(userId, realmId, provider, report, response);

        try {
            String accountNumber = safeJsonPathRead(report, "$.accountXns[0].accountNo", String.class)
                    .orElseThrow(() -> new IllegalArgumentException("No account number found in report"));


            List<String> totalAccountsInReport = safeJsonPathRead(report, "$.accountXns[*].accountNo", List.class)
                    .map(value -> (List<String>) value)
                    .orElse(Collections.emptyList());

            int index = totalAccountsInReport.indexOf(accountNumber);
            if (index==-1) {
                log.warn("Account number {} not found in accountXns for userId: {}", accountNumber, userId);
                index = 0; //Default
            }

            BankStatementHashType hashType = BankStatementHashType.BANK_TRANSACTION;

            JsonNode rootNode = objectMapper.readTree(response);

            List<String> transactions = determineTransactionsList(provider, hashType, rootNode, accountNumber);


//            List<Map<String, Object>> matchingBankStatements = findMatchingBankStatements(userId, accountNumber, merkleRootHash);
            List<Map<String, Object>> matchedBankTransactions = findMatchingBankTransactions(userId, hashType.ordinal(), transactions);
            log.debug("Found {} matching transactions for userId: {}", matchedBankTransactions.size(), userId);

            Map<String, Object> output = generateResponse(userId, realmId, report, response, matchedBankTransactions,
                    accountNumber, transactions.size(), index);
            log.info("Dedupe response generated successfully for userId: {}", userId);

            return output;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid data for dedupe fetch - userId: {}, error: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch dedupe details for userId: {}, realmId: {}, provider: {}",
                    userId, realmId, provider, e);
            throw new RuntimeException("Error fetching dedupe details: " + e.getMessage(), e);
        }
    }

    private void validateInputs(Integer userId, String realmId, Provider provider, String report, String response) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(realmId, "realmId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(report, "report must not be null");
        Objects.requireNonNull(response, "response must not be null");
    }

    private List<String> determineTransactionsList(Provider provider, BankStatementHashType hashType,
                                                   JsonNode rootNode, String accountNumber) {
        try {
            Map<String, List<String>> tokens = extractTokens(rootNode, hashTypeToSection(hashType));
            List<String> transactionTokens = tokens.getOrDefault(accountNumber, Collections.emptyList());
            if (transactionTokens.isEmpty()) {
                log.debug("No transactions found for account: {} with hashType: {}", accountNumber, hashType);
            }
            return transactionTokens;
        } catch (Exception e) {
            log.error("Error determining transactions list for account: {}, hashType: {}", accountNumber, hashType, e);
            return Collections.emptyList();
        }
    }

    private String hashTypeToSection(BankStatementHashType hashType) {
        return switch (hashType) {
            case ACCOUNT_XNS -> "accountXns";
            case BANK_TRANSACTION -> "bankTransactions";
            case EOD_BALANCE -> "eod";
        };
    }


    public List<Map<String, Object>> findMatchingBankStatements(Integer userId, String accountNumber, String merkleRootHash, Integer hashType) {
        String sql = """
                    SELECT * FROM public.user_bank_statement
                    WHERE root_hash = ?
                    AND hash_type = ?
                    AND (user_id != ? OR account_number != ?)
                """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, merkleRootHash, hashType, userId, accountNumber);

            log.debug("Found {} matching bank statements for userId: {}, accountNumber: {}",
                    results.size(), userId, accountNumber);
            return results.isEmpty() ? Collections.emptyList():results;
        } catch (Exception e) {
            log.error("Error fetching matching bank statements for userId: {}, accountNumber: {}",
                    userId, accountNumber, e);
            throw new RuntimeException("Error fetching matching bank statements: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> findMatchingBankTransactions(Integer userId, Integer hashType, List<String> transactions) {
        if (transactions==null || transactions.isEmpty()) {
            log.debug("No transactions provided to match for userId: {}", userId);
            return Collections.emptyList();
        }
        String sql = """
                    WITH hash_list AS (
                        SELECT unnest(?::text[]) AS hash
                    )
                    SELECT *
                    FROM user_bank_transaction u
                    JOIN hash_list t
                    ON u.hash = t.hash
                    WHERE u.hash_type = ? AND u.user_id != ?;
                """;

        try {
            Array hashArray = jdbcTemplate.getDataSource()
                    .getConnection()
                    .createArrayOf("text", transactions.toArray());

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, hashArray, hashType, userId);
            log.debug("Found {} matching transactions for userId: {}", results.size(), userId);
            return results;
        } catch (Exception e) {
            log.error("Error fetching matching bank transactions for userId: {}, hashType: {}", userId, hashType, e);
            throw new RuntimeException("Error fetching matching bank transactions: " + e.getMessage(), e);
        }
    }


    public Map<String, Object> generateResponse(Integer userId, String realmId, String report, String response, List<Map<String, Object>> matchedTransactions, String accountNo, Integer totalTransactions, int index) throws Exception {

        try {
            Map<String, Object> finalResponse = new LinkedHashMap<>();

            finalResponse.put("userId", userId);
            finalResponse.put("realmId", realmId);
            finalResponse.put("name", safeJsonPathRead(report, String.format("$.customerDetails[%d].name", index), String.class).orElse(""));
            finalResponse.put("pan", safeJsonPathRead(report, String.format("$.customerDetails[%d].pan", index), String.class).orElse(""));
            finalResponse.put("aadharMasked", safeJsonPathRead(report, String.format("$.customerDetails[%d].aadharMasked", index), String.class).orElse(""));
            finalResponse.put("bankName", safeJsonPathRead(report, String.format("$.customerDetails[%d].bankName", index), String.class).orElse(""));


            String currentOverallStatus = "GREEN";

            Map<String, List<Map<String, Object>>> groupedByAccount = matchedTransactions.stream()
                    .collect(Collectors.groupingBy(txn -> txn.get("account_number").toString()));

            List<Map<String, Object>> accounts = new ArrayList<>();
            List<Map<String, Object>> otherAccounts = fetchOtherAccounts(userId, realmId);
            List<Map<String, Object>> transactions = new ArrayList<>();
            List<Map<String, Object>> statements = new ArrayList<>();

            int totalMatchedCount = matchedTransactions.size();

            for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByAccount.entrySet()) {
                String accountNumber = entry.getKey();
                List<Map<String, Object>> transactionsForAccount = groupedByAccount.get(accountNumber);

                int totalCountForAccount = transactionsForAccount.size();
                int totalCountForRequestedAccount = totalTransactions;

                double matchPercentage = ((double) totalCountForAccount / totalCountForRequestedAccount) * 100;

                currentOverallStatus = determineOverallStatus(currentOverallStatus, matchPercentage);
                DedupeStatus dedupeStatus = DedupeStatus.fromMatchPercentage(matchPercentage);

                if (!transactionsForAccount.isEmpty()) {
                    Map<String, Object> firstTxn = transactionsForAccount.get(0);
                    transactions.add(buildTransactionData(firstTxn, dedupeStatus, matchPercentage, totalCountForAccount));
                    statements.add(buildStatementData(firstTxn, dedupeStatus, matchPercentage));
                }
            }

            Map<String, Object> accountMap = new LinkedHashMap<>();
            accountMap.put("accountNumber", accountNo);
            accountMap.put("bankName", safeJsonPathRead(report, String.format("$.customerDetails[%d].bankName", index), String.class).orElse(""));
            accounts.add(accountMap);

            finalResponse.put("status", currentOverallStatus);
            finalResponse.put("accounts", accounts);
            finalResponse.put("otherAccounts", otherAccounts);

            if (!statements.isEmpty()) {
                finalResponse.put("statements", statements);
            }

            if (!transactions.isEmpty()) {
                finalResponse.put("transactions", transactions);
            }

            if (statements.isEmpty() && transactions.isEmpty()) {
                finalResponse.put("remarks", String.format("No matching records found for the user %s.", userId));
            }

            return finalResponse;
        } catch (Exception e) {
            log.error("Error generating dedupe response for userId: {}", userId, e);
            throw new RuntimeException("Error generating response: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> fetchOtherAccounts(Integer userId, String realmId) {
        String sql = """
        SELECT account_number, bank_name
        FROM public.user_bank_details
        WHERE user_id = ? AND realm_id = ?;
        """;
        try {
            return jdbcTemplate.queryForList(sql, userId, realmId);
        } catch (Exception e) {
            log.error("Error fetching other accounts for userId: {}, realmId: {}", userId, realmId, e);
            return Collections.emptyList();
        }
    }


    private String determineOverallStatus(String currentStatus, double matchPercentage) {
        if (matchPercentage >= 75) return "RED";
        if (matchPercentage >= 50 && !"RED".equals(currentStatus)) return "AMBER";
        return currentStatus;
    }

    private Map<String, Object> buildTransactionData(Map<String, Object> txn, DedupeStatus dedupeStatus, double matchPercentage, Integer noOfMatchTransactions) {
        Map<String, Object> transactionData = new LinkedHashMap<>();
        Integer hashTypeOrdinal = (Integer) txn.get("hash_type");
        transactionData.put("section", BankStatementHashType.fromOrdinal(hashTypeOrdinal));
        transactionData.put("status", dedupeStatus.getStatus());

        Map<String, Object> counterPartyData = new LinkedHashMap<>();
        counterPartyData.put("counterPartyUserId", txn.getOrDefault("user_id", "").toString());
        counterPartyData.put("counterPartyRealmId", txn.getOrDefault("realm_id", "").toString());
        counterPartyData.put("counterPartyAccountNumber", txn.getOrDefault("account_number", "").toString());
        counterPartyData.put("matchScore", matchPercentage);
        counterPartyData.put("matchType", dedupeStatus.getMatchType());

        transactionData.put("matchedCounterParty", List.of(counterPartyData));
        transactionData.put("riskLevel", dedupeStatus.getRiskLevel());
        transactionData.put("reasonForFlagging", dedupeStatus.getReasonForFlagging());
        transactionData.put("recommendation", dedupeStatus.getRecommendation());
        transactionData.put("noOfMatchedTransactions", noOfMatchTransactions);
        return transactionData;
    }

    private Map<String, Object> buildStatementData(Map<String, Object> txn, DedupeStatus dedupeStatus, double matchPercentage) {
        Map<String, Object> statementData = new LinkedHashMap<>();
        Integer hashTypeOrdinal = (Integer) txn.get("hash_type");
        statementData.put("section", BankStatementHashType.fromOrdinal(hashTypeOrdinal));
        statementData.put("status", dedupeStatus.getStatus());

        Map<String, Object> counterPartyData = new LinkedHashMap<>();
        counterPartyData.put("counterPartyUserId", txn.getOrDefault("user_id", "").toString());
        counterPartyData.put("counterPartyRealmId", txn.getOrDefault("realm_id", "").toString());
        counterPartyData.put("counterPartyAccountNumber", txn.getOrDefault("account_number", "").toString());
        counterPartyData.put("matchScore", matchPercentage);
        counterPartyData.put("matchType", dedupeStatus.getMatchType());

        statementData.put("matchedCounterParty", List.of(counterPartyData));
        statementData.put("riskLevel", dedupeStatus.getRiskLevel());
        statementData.put("reasonForFlagging", dedupeStatus.getReasonForFlagging());
        statementData.put("recommendation", dedupeStatus.getRecommendation());
        return statementData;
    }

    private Map<String, List<String>> extractTokens(JsonNode rootNode, String section) {
        Map<String, List<String>> result = new HashMap<>();

        try {
            if (!rootNode.has(section)) {
                log.debug("Section {} not found in response", section);
            }
            for (JsonNode accountNode : rootNode.get(section)) {
                String accountNo = accountNode.path("accountNo").asText(null);
                if (accountNo==null) continue;

                List<String> tokens = new ArrayList<>();
                String field = switch (section) {
                    case "accountXns" -> "xns";
                    case "eod" -> "balance";
                    case "bankTransactions" -> "transactions";
                    default -> null;
                };

                if (field != null && accountNode.has(field)) {
                    for (JsonNode subNode : accountNode.get(field)) {
                        if (subNode.has("token")) {
                            tokens.add(subNode.get("token").asText());
                        }
                    }
                }
                if (!tokens.isEmpty()) {
                    result.put(accountNo, tokens);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Error Extracting tokens for section: {}", section, e);
            return result;
        }
    }

    public static <T> Optional<T> safeJsonPathRead(String json, String path, Class<T> type) {
        try {
            Object result = JsonPath.read(json, path);
            if (result!=null && type.isInstance(result)) {
                return Optional.of(type.cast(result));
            }
            log.debug("No valid data found at JSON path: {}", path);
        } catch (Exception e) {
            log.warn("Failed to read JSON path '{}': {}", path, e.getMessage());
        }
        return Optional.empty();
    }

}

