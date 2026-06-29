package com.grupo3aor.innovationlab.audit;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing {@link AuditLog} entities.
 * Provides standard CRUD operations and database access for audit logs.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
