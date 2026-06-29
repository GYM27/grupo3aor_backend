package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Inbound Data Transfer Object for creating or updating a Clinical Scenario.
 * <p>
 * I built this request wrapper to filter incoming API payloads, guaranteeing 
 * that malicious users cannot inject administrative flags (like the active state) 
 * during scenario generation.
 * </p>
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClinicalScenarioRequest {

    /**
     * The title of the clinical scenario.
     */
    @NotBlank(message = "Scenario name cannot be empty")
    @Size(max = 150, message = "Scenario name cannot exceed 150 characters")
    private String name;

    /**
     * Detailed medical description of the scenario.
     */
    // I mapped this description to a TEXT column using Lob to allow 
    // extensive medical documentation without breaking the string length boundaries.
    private String description;

    /**
     * Medical Device Information Base (MDIB) standard version.
     */
    private Integer mdibVersion;

    /**
     * Identifier of the source medical device.
     */
    private String device;

    /**
     * List of physiological readings included in the clinical scenario.
     */
    private List<MetricDTO> metrics;
}
