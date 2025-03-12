package com.ninja.BankStAnalysis.core.port.out;

import java.util.List;

public interface ResponsePersistRepositoryPort {
    void insertUserBankStatementFlagStatus(List<Object[]> batchData);
}
