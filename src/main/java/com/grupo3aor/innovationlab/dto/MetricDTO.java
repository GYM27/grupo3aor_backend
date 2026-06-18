package com.grupo3aor.innovationlab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * Data Transfer Object (DTO) for mapping individual metric readings 
 * extracted from the clinical scenario JSON file.
 * Each MetricDTO represents a specific point in time with a physiological value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricDTO {
    private String handle;
    private String unit;
    private Double value;
    /** Expected format: "2026-03-17T10:00:05Z" */
    private String timestamp;
}
