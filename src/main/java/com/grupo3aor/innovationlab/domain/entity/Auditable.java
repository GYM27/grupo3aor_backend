package com.grupo3aor.innovationlab.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Unified base class centralizing audit tracking columns for all persistent entities.
 * <p>
 * I refactored this superclass to use Hibernate-native timestamp annotations instead of
 * Spring Data JPA auditing, eliminating the dependency on {@code @EnableJpaAuditing} and
 * ensuring timestamps are populated automatically without additional configuration beans.
 * I also switched to {@code @SuperBuilder} to allow child entities to set inherited audit
 * fields directly through their builders.
 * </p>
 * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Auditable {

    /**
     * Instant when the record was originally stored.
     */
    // I chose @CreationTimestamp (Hibernate-native) over @CreatedDate (Spring Data)
    // to guarantee automatic population without requiring @EnableJpaAuditing configuration.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Instant when the record was last modified.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Operator identity who inserted the record.
     */
    // I decided to keep createdBy/updatedBy as manually-set String fields rather than
    // relying on @CreatedBy/@LastModifiedBy, which would require an AuditorAware bean.
    // Our Services inject the operator email explicitly, giving us full control.
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /**
     * Operator identity who made the last change.
     */
    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * Client physical network address origin.
     */
    // I placed this field in the superclass so every table in the system inherits
    // the network traceability column as specified in the UML diagram.
    @Column(name = "origin_ip")
    private String originIp;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        String currentUser = getCurrentUser();
        if (this.createdBy == null) {
            this.createdBy = currentUser;
        }
        this.updatedBy = currentUser;
        
        if (this.originIp == null) {
            this.originIp = getCurrentIp();
        }
    }

    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedBy = getCurrentUser();
        this.originIp = getCurrentIp();
    }

    private String getCurrentUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) ? auth.getName() : "SYSTEM";
    }

    private String getCurrentIp() {
        org.springframework.web.context.request.ServletRequestAttributes attrs = 
            (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        return (attrs != null && attrs.getRequest() != null) ? attrs.getRequest().getRemoteAddr() : "SYSTEM";
    }
}
