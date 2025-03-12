package com.ninja.BankStAnalysis.infra.adapter.extractor;


import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import com.ninja.BankStAnalysis.core.port.out.StatementAdapter;

public interface BankStatementExtractor extends StatementAdapter {
    boolean supports(Provider provider); // Determines if this extractor supports the given provider
}