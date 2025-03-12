package com.ninja.BankStAnalysis.infra.exceptions;

public class ServiceRuntimeException extends RuntimeException {
    private final ErrorCode errorCode;

    public ServiceRuntimeException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ServiceRuntimeException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}