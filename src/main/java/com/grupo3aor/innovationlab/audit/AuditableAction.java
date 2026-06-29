package com.grupo3aor.innovationlab.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation used to mark methods that perform sensitive actions
 * requiring an audit log entry.
 * <p>
 * The {@link AuditAspect} intercepts methods annotated with this annotation
 * and automatically records the action description, the authenticated user's email,
 * the origin IP address, and the timestamp of the execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableAction {
    /**
     * Defines the name or description of the action being performed.
     * This value will be stored in the audit log.
     *
     * @return the description of the action (e.g., "CREATE_PROJECT", "DELETE_SCENARIO")
     */
    String action();
}
