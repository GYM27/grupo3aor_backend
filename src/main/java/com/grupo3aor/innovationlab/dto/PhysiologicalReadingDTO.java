package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object containing constraint definitions for physiological inputs.
 */
public class PhysiologicalReadingDTO {

    private UUID id;

    @NotNull(message = "Simulation ID cannot be null")
    private UUID simulationId;

    @NotBlank(message = "Measurement handle cannot be empty")
    private String handle;

    @NotBlank(message = "Measurement unit cannot be empty")
    private String unit;

    @NotNull(message = "Reading value is required")
    private BigDecimal value;

    private LocalDateTime timestamp;

    public PhysiologicalReadingDTO() {
    }

    // --- Getters and Setters ---
    public UUID getId() { return id; }

    public void setId(UUID id) { this.id = id; }

    public UUID getSimulationId() { return simulationId; }

    public void setSimulationId(UUID simulationId) { this.simulationId = simulationId; }

    public String getHandle() { return handle; }

    public void setHandle(String handle) { this.handle = handle; }

    public String getUnit() { return unit; }

    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getValue() { return value; }

    public void setValue(BigDecimal value) { this.value = value; }

    public LocalDateTime getTimestamp() { return timestamp; }

    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}