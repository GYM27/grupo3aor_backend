package com.grupo3aor.innovationlab.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Database entity mapped to capture runtime threshold violation alerts.
 *
 */
@Entity
@Table(name = "ALERT")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "simulation_id", nullable = false)
    private UUID simulationId;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status;

    @Column(name = "value_at_trigger", nullable = false, precision = 10, scale = 2)
    private BigDecimal valueAtTrigger;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy; // Changed to Long to match group implementation

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy; // Changed to Long to match group implementation

    @Column(name = "origin_ip", nullable = false)
    private String originIp;

    public Alert() {
    }

    // --- Getters and Setters ---
    public UUID getId() { return id; }

    public void setId(UUID id) { this.id = id; }

    public UUID getSimulationId() { return simulationId; }

    public void setSimulationId(UUID simulationId) { this.simulationId = simulationId; }

    public UUID getRuleId() { return ruleId; }

    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public LocalDateTime getTimestamp() { return timestamp; }

    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public AlertStatus getStatus() { return status; }

    public void setStatus(AlertStatus status) { this.status = status; }

    public BigDecimal getValueAtTrigger() { return valueAtTrigger; }

    public void setValueAtTrigger(BigDecimal valueAtTrigger) { this.valueAtTrigger = valueAtTrigger; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getCreatedBy() { return createdBy; }

    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getUpdatedBy() { return updatedBy; }

    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public String getOriginIp() { return originIp; }

    public void setOriginIp(String originIp) { this.originIp = originIp; }
}