package com.ninja.BankStAnalysis.core.port.out;

import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import org.springframework.web.multipart.MultipartFile;

public interface BankStatementExtractionPort {
    String extractReport(Provider provider, MultipartFile bankStatement) throws Exception;
}
