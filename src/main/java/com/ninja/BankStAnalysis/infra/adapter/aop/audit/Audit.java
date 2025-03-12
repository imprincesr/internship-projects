package com.ninja.BankStAnalysis.infra.adapter.aop.audit;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class Audit {
    private String id;
    private String provider;
    private String realmId;
    private String userId;
    private Object context;
    private String apiCode;
    private boolean success;
    private Object request;
    private Object response;
    private Object error;
    private Long timestamp;
}