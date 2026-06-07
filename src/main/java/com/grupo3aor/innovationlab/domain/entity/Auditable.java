package com.grupo3aor.innovationlab.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base class for all entities that need audit tracking.
 * <p>
 * I created this to automatically handle our timestamps and user tracking.
 * It strictly uses String for createdBy/updatedBy to stay fully compatible 
 * with the structure of the User entity!
 * </p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Using String to perfectly match the 'createdBy' in User.java
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    // Using String to perfectly match the 'updatedBy' in User.java
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    // We can't auto-fill this with pure Spring Data JPA natively, 
    // but I added it here so all tables inherit this column as requested in the UML.
    @Column(name = "origin_ip")
    private String originIp;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getOriginIp() {
        return originIp;
    }

    public void setOriginIp(String originIp) {
        this.originIp = originIp;
    }
}
