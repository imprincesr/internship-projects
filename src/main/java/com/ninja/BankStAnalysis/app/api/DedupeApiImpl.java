package com.ninja.BankStAnalysis.app.api;

import com.ninja.BankStAnalysis.core.port.in.DedupeServicePort;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/realms/{realmId}/users/{userId}")
@RequiredArgsConstructor
@Slf4j
public class DedupeApiImpl {

    private final DedupeServicePort dedupeServicePort;

    @PostMapping(value = "/bankStatements/dedupe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> dedupe(
            @PathVariable("realmId") @NotNull String realmId,
            @PathVariable("userId") @NotNull Integer userId,
            @RequestParam("bankStatement") @NotNull MultipartFile bankStatement) {

        log.info("Starting dedupe process for userId: {} in realmId: {}", userId, realmId);

        try {
            validateInput(bankStatement);
            Map<String, Object> matchingStatements = dedupeServicePort.processBankStatement(userId, realmId, bankStatement);

            log.info("Dedupe completed successfully for userId: {}.",
                    userId);
            return ResponseEntity.ok(matchingStatements);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for dedupe request - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid request: " + e.getMessage(),
                            "status", "BAD_REQUEST"));

        } catch (Exception e) {
            log.error("Failed to process dedupe request for userId: {}, realmId: {}", userId, realmId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "An unexpected error occurred while processing the bank statement",
                            "status", "INTERNAL_SERVER_ERROR"));
        }
    }

    private void validateInput(MultipartFile bankStatement) {
        if (bankStatement == null || bankStatement.isEmpty()) {
            throw new IllegalArgumentException("Bank statement file cannot be null or empty");
        }
        // Add more validation as needed (e.g., file size, content type)
        if (bankStatement.getSize() > 10 * 1024 * 1024) { // Example: 10MB limit
            throw new IllegalArgumentException("Bank statement file size exceeds 10MB limit");
        }
    }
}