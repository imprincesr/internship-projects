package com.ninja.BankStAnalysis.app.api;

import com.ninja.BankStAnalysis.core.port.in.BankStAnalysisServicePort;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/realms/{realmId}/users/{userId}")
@RequiredArgsConstructor
@Slf4j
public class BankStAnalysisApiImpl {

    private final BankStAnalysisServicePort bankStAnalysisServicePort;

    @PostMapping(value = "/bankStatements", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Map<String, Object>>> batchInsert(
            @PathVariable("realmId") @NotNull String realmId,
            @PathVariable("userId") @NotNull Integer userId,
            @RequestParam("bankStatement") @NotNull MultipartFile bankStatement) {

        log.info("Processing bank statement for userId: {} in realmId: {}", userId, realmId);

        try {
            validateInput(bankStatement);
            List<Map<String, Object>> ingestedStatements = bankStAnalysisServicePort.processBankStatement(userId, realmId, bankStatement);
            log.info("Bank statement processed successfully for userId: {}, inserted {} statements", userId, ingestedStatements.size());
            return ResponseEntity.ok(ingestedStatements);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for userId: {} - {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Collections.singletonList(Map.of(
                            "error", "Invalid request",
                            "message", e.getMessage(),
                            "status", "BAD_REQUEST"
                    )));

        } catch (Exception e) {
            log.error("Error processing bank statement for userId: {} in realmId: {}", userId, realmId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonList(Map.of(
                            "error", "Internal server error",
                            "message", "An unexpected error occurred while processing the file",
                            "status", "INTERNAL_SERVER_ERROR"
                    )));
        }
    }

    private void validateInput(MultipartFile bankStatement) {
        if (bankStatement == null || bankStatement.isEmpty()) {
            throw new IllegalArgumentException("Bank statement file cannot be null or empty");
        }
        // Add other validations if want
        if (bankStatement.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new IllegalArgumentException("Bank statement file size exceeds 10MB limit");
        }
    }
}