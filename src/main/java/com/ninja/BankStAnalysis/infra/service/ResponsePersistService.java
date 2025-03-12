package com.ninja.BankStAnalysis.infra.service;

import com.jayway.jsonpath.JsonPath;
import com.ninja.BankStAnalysis.core.port.in.ResponsePersistServicePort;
import com.ninja.BankStAnalysis.core.port.out.ResponsePersistRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponsePersistService implements ResponsePersistServicePort {

    private final JdbcTemplate jdbcTemplate;
    private final ResponsePersistRepositoryPort responsePersistRepositoryPort;

    @Transactional
    public void persistResponse(String response) {
        try {
            log.info("Starting persistence of response for user data");

            // Extract core fields with fallbacks
            Integer userId = safeJsonPathRead(response, "$.userId", Integer.class)
                    .orElseThrow(() -> new IllegalArgumentException("userId is required"));
            String realmId = safeJsonPathRead(response, "$.realmId", String.class).orElse("").trim();
            String flag = safeJsonPathRead(response, "$.status", String.class).orElse("UNKNOWN");
            String accountNumber = safeJsonPathRead(response, "$.accounts[0].accountNumber", String.class)
                    .orElse("");

            if (accountNumber.isEmpty()) {
                log.warn("No account number found for userId: {}", userId);
            }

            // Extract matched counterparty data with empty list fallback
            List<String> matchedCounterPartyUserIdsRaw = safeJsonPathRead(response,
                    "$.statements[*].matchedCounterParty[*].counterPartyUserId", List.class)
                    .orElse(Collections.emptyList());
            List<Integer> matchedCounterPartyUserIds = parseCounterPartyUserIds(matchedCounterPartyUserIdsRaw);

            List<String> matchedCounterPartyAccountNos = safeJsonPathRead(response,
                    "$.statements[*].matchedCounterParty[*].counterPartyAccountNumber", List.class)
                    .orElse(Collections.emptyList());

            // Audit timestamps
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime flaggedOn = now;
            LocalDateTime createdAt = now;
            LocalDateTime verifiedOn = now;
            String createdBy = "SYSTEM"; // Consider making this configurable
            LocalDateTime updatedAt = null; //Initial update at is null
            String updatedBy = null;     // Initial update by is null

            // Prepare data for batch insert
            List<Object[]> batchData = Collections.singletonList(new Object[]{
                    userId, realmId, flag, flaggedOn, accountNumber,
                    matchedCounterPartyUserIds.toArray(new Integer[0]),
                    matchedCounterPartyAccountNos.toArray(new String[0]),
                    verifiedOn, createdAt, createdBy, updatedAt, updatedBy
            });

            responsePersistRepositoryPort.insertUserBankStatementFlagStatus(batchData);
            log.info("Successfully persisted response for userId: {}", userId);

        } catch (Exception e) {
            log.error("Failed to persist response: {}", e.getMessage(), e);
            throw new RuntimeException("Persistence operation failed", e);
        }
    }

    private List<Integer> parseCounterPartyUserIds(List<String> rawIds) {
        List<Integer> parsedIds = new ArrayList<>();
        if (rawIds == null || rawIds.isEmpty()) {
            return parsedIds;
        }

        for (String id : rawIds) {
            try {
                parsedIds.add(Integer.parseInt(id.trim()));
            } catch (NumberFormatException e) {
                log.warn("Invalid counterPartyUserId '{}', skipping: {}", id, e.getMessage());
            }
        }
        return parsedIds;
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


}
