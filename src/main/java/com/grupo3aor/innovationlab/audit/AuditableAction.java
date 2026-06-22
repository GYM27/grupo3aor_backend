package com.grupo3aor.innovationlab.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that perform actions requiring an audit log.
 * The AuditAspect will intercept these methods and log the action, the user, and the IP address.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableAction {
    /**
     * The name or description of the action being performed (e.g., "DELETE_SCENARIO").
     * @return the action description
     */
    String action();
}
