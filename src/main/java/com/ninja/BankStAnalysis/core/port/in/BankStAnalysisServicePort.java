package com.ninja.BankStAnalysis.core.port.in;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface BankStAnalysisServicePort {
    List<Map<String, Object>> processBankStatement(Integer userId, String realmId, MultipartFile bankStatement);
}
