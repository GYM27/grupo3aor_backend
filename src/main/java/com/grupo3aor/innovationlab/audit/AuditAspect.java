package com.grupo3aor.innovationlab.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect responsible for intercepting methods annotated with {@link AuditableAction}.
 * It captures execution context (user, IP address, method signature) and persists
 * an audit log entry in the database.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    /**
     * Intercepts successful method executions annotated with {@link AuditableAction}
     * and logs the corresponding audit activity.
     *
     * @param joinPoint       the join point representing the intercepted method execution
     * @param auditableAction the annotation containing the action description
     */
    @AfterReturning("@annotation(auditableAction)")
    public void logAuditActivity(JoinPoint joinPoint, AuditableAction auditableAction) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = (auth != null && auth.isAuthenticated()) ? auth.getName() : "SYSTEM";

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String originIp = "UNKNOWN";
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                originIp = request.getRemoteAddr();
            }

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodString = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

            AuditLog auditLog = AuditLog.builder()
                    .action(auditableAction.action())
                    .userEmail(userEmail)
                    .originIp(originIp)
                    .methodSignature(methodString)
                    .build();

            auditLogRepository.save(auditLog);

            log.info("[AUDIT] Action: {}, User: {}, IP: {}, Method: {}", 
                    auditableAction.action(), userEmail, originIp, methodString);

        } catch (Exception e) {
            log.error("[AUDIT ERROR] Failed to record audit log: {}", e.getMessage());
        }
    }
}
