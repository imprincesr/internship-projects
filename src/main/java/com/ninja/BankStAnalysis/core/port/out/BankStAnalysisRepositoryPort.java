package com.ninja.BankStAnalysis.core.port.out;

import com.ninja.BankStAnalysis.core.modelHelper.Provider;

import java.util.List;
import java.util.Map;

public interface BankStAnalysisRepositoryPort {
    List<Map<String, Object>> saveDetails(Integer userId, String realmId, Provider provider, String report, String summaryJson);
}
