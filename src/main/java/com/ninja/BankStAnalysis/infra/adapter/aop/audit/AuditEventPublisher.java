package com.ninja.BankStAnalysis.infra.adapter.aop.audit;


import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class AuditEventPublisher {
    private static final Logger logger = Logger.getLogger(AuditEventPublisher.class.getName());

    public void publishEvent(Audit event) {
        // Log the audit event to a file or console
        logger.info(event.toString());
    }

    public void createEventAndPublish(String provider, String apiName, String realmId, String userId,
                                      boolean success, Object request, Object response, Object error, Object... context) {
        Audit auditEvent = Audit.builder()
                .provider(provider)
                .apiCode(apiName)
                .realmId(realmId)
                .userId(userId)
                .context(context)
                .success(success)
                .request(request)
                .response(response)
                .error(error)
                .build();
        publishEvent(auditEvent);
    }
}