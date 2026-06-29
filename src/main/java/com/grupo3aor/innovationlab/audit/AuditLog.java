package com.grupo3aor.innovationlab.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an immutable audit log record in the system.
 * This class stores information about actions performed by users,
 * including their email, origin IP address, and the specific method executed.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    /**
     * The unique identifier for the audit log entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * A descriptive string representing the action performed (e.g., "CREATE_USER").
     */
    @Column(nullable = false)
    private String action;

    /**
     * The email of the user who performed the action.
     * If the action was performed by an unauthenticated source, it defaults to "SYSTEM".
     */
    @Column(nullable = false)
    private String userEmail;

    /**
     * The IP address from which the request originated.
     */
    @Column(nullable = false)
    private String originIp;

    /**
     * The signature of the method that was executed (Class.method).
     */
    @Column(nullable = false)
    private String methodSignature;

    /**
     * The timestamp indicating when the action occurred.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
