package com.ninja.BankStAnalysis.infra.adapter.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import com.ninja.BankStAnalysis.core.port.out.BankStatementExtractionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BankStatementExtractorContext implements BankStatementExtractionPort {

    private final List<BankStatementExtractor> extractors; // All strategy implementations
    private final ObjectMapper objectMapper;

    private BankStatementExtractor getExtractor(Provider provider, MultipartFile bankStatement) throws Exception {
        // Special case for Perfios with "report" field
        if (provider == Provider.PERFIOS) {
            JsonNode rootNode = objectMapper.readTree(bankStatement.getInputStream());
            if (rootNode.has("report")) {
                return extractors.stream()
                        .filter(extractor -> extractor instanceof PerfiosNinjacartExtractor)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("PerfiosNinjacartExtractor not found"));
            }
        }

        // General case
        return extractors.stream()
                .filter(extractor -> extractor.supports(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No extractor found for provider: " + provider));
    }

    @Override
    public String extractReport(Provider provider, MultipartFile bankStatement) throws Exception {
        BankStatementExtractor extractor = getExtractor(provider, bankStatement);
        return extractor.processStatement(bankStatement);
    }
}
