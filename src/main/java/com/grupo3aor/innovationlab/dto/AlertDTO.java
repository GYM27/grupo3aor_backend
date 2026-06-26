package com.grupo3aor.innovationlab.dto;

import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Contract applied to operational system alert transmissions.
 * I replaced the manual getters/setters with Lombok @Data to align with the
 * coding style used across all other DTOs in the project.
 */
@Data
public class AlertDTO {

    private UUID id;

    @NotNull(message = "Associated simulation ID cannot be null")
    private UUID simulationId;

    @NotNull(message = "Triggering rule ID cannot be null")
    private UUID ruleId;

    private LocalDateTime timestamp;
    private AlertStatus status;

    private String severity;
    private String systemName;
    private String ruleName;
    private String analyticalJustification;
    private String formattedValue;

    @NotNull(message = "The reading value recorded at trigger time is required")
    private Double valueAtTrigger;
}
