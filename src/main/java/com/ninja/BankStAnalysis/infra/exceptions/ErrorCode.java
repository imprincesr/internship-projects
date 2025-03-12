package com.ninja.BankStAnalysis.infra.exceptions;


public enum ErrorCode {
    RS_ERROR_0001("Invalid request parameters"),
    RS_ERROR_0002("SourceObjectId mismatch with userId");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}