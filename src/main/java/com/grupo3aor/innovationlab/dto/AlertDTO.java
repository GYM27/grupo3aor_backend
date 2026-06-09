package com.grupo3aor.innovationlab.dto;

import com.grupo3aor.innovationlab.domain.entity.AlertStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Contract applied to operational system alert transmissions.
 */
public class AlertDTO {

    private UUID id;

    @NotNull(message = "Associated simulation ID cannot be null")
    private UUID simulationId;

    @NotNull(message = "Triggering rule ID cannot be null")
    private UUID ruleId;

    private LocalDateTime timestamp;
    private AlertStatus status;

    @NotNull(message = "The reading value recorded at trigger time is required")
    private BigDecimal valueAtTrigger;

    public AlertDTO() {
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
}