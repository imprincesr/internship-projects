package com.ninja.BankStAnalysis.core.port.out;

import org.springframework.web.multipart.MultipartFile;

public interface StatementAdapter {
    String processStatement(MultipartFile bankStatement) throws Exception;
}
