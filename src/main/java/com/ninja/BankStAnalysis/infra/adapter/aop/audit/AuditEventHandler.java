package com.ninja.BankStAnalysis.infra.adapter.aop.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventHandler {

    public void handleAuditEvent(Audit event) {
        // Log the audit event to the console
        log.info(event.toString());
    }
}