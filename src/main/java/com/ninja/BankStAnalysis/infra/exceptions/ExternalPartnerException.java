package com.ninja.BankStAnalysis.infra.exceptions;

public class ExternalPartnerException extends RuntimeException {
    private Object[] params;

    public ExternalPartnerException(String message, Object[] params) {
        super(message);
        this.params = params;
    }

    public Object[] getParams() {
        return params;
    }
}