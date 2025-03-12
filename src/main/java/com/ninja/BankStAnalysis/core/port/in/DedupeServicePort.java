package com.ninja.BankStAnalysis.core.port.in;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface DedupeServicePort {
    Map<String, Object> processBankStatement(Integer userId, String realmId, MultipartFile bankStatement);
}

