package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object containing constraint definitions for physiological inputs.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PhysiologicalReadingDTO {

    private UUID id;

    @NotNull(message = "Simulation ID cannot be null")
    private UUID simulationId;

    @NotBlank(message = "Measurement handle cannot be empty")
    private String handle;

    @NotBlank(message = "Measurement unit cannot be empty")
    private String unit;

    @NotNull(message = "Reading value is required")
    private Double value;

    private LocalDateTime timestamp;
}