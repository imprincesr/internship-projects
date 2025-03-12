package com.ninja.BankStAnalysis.infra.adapter.aop;

import com.ninja.BankStAnalysis.infra.adapter.aop.audit.AuditEventPublisher;
import com.ninja.BankStAnalysis.infra.exceptions.ExternalPartnerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalApiAuditAspect {
    private final AuditEventPublisher auditEventPublisher;

    @Around("@annotation(externalApiAudit)")
    public Object around(ProceedingJoinPoint pjp, ExternalApiAudit externalApiAudit) throws Throwable {
        Object proceed = pjp.proceed();
        List<Object> args = Arrays.stream(pjp.getArgs()).toList();
        auditEventPublisher.createEventAndPublish(
                externalApiAudit.provider().toString(),
                externalApiAudit.apiName(),
                (String) args.get(0),
                (String) args.get(1),
                true,
                args.get(2),
                proceed,
                null,
                args.subList(3, args.size()).toArray()
        );
        return proceed;
    }

    @AfterThrowing(value = "@annotation(externalApiAudit)", throwing = "ex")
    public void afterThrowing(JoinPoint jp, ExternalApiAudit externalApiAudit, Exception ex) {
        List<Object> args = List.of(jp.getArgs());

        String customErrorMessage = "";
        if (ex instanceof ExternalPartnerException e && Objects.nonNull(e.getParams())
                && e.getParams().length > 0 && !(e.getParams()[0] instanceof Throwable)) {
            customErrorMessage = String.valueOf(e.getParams()[0]);
        }
        Map<String, Object> error = new HashMap<>();
        error.put("exception", ex.toString());
        error.put("message", ex.getMessage());
        error.put("customErrorMessage", customErrorMessage);
        auditEventPublisher.createEventAndPublish(
                externalApiAudit.provider().toString(),
                externalApiAudit.apiName(),
                (String) args.get(0),
                (String) args.get(1),
                false,
                args.get(2),
                null,
                error,
                args.subList(3, args.size()).toArray()
        );
    }
}