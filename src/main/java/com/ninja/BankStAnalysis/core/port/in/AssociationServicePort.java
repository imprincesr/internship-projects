package com.ninja.BankStAnalysis.core.port.in;

import org.springframework.web.multipart.MultipartFile;

public interface AssociationServicePort {
    void processBankStatement(Integer userId, String realmId, MultipartFile bankStatement);
}
