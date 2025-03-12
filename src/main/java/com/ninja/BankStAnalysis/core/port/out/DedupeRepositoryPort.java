package com.ninja.BankStAnalysis.core.port.out;

import com.ninja.BankStAnalysis.core.modelHelper.Provider;

import java.util.Map;

public interface DedupeRepositoryPort {
    Map<String, Object> fetchDetails(Integer userId, String realmId, Provider provider, String report, String summaryJson);
}
