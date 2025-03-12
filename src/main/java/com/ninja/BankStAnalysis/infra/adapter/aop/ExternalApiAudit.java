package com.ninja.BankStAnalysis.infra.adapter.aop;


import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalApiAudit {
    Provider provider();
    String apiName();
}