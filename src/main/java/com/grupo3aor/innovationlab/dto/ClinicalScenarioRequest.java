package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Inbound Data Transfer Object for creating or updating a Clinical Scenario.
 * <p>
 * I built this request wrapper to filter incoming API payloads, guaranteeing 
 * that malicious users cannot inject administrative flags (like the active state) 
 * during scenario generation.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
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
    // I allowed this description to be submitted without strict upper string limits,
    // matching the Lob TEXT column capacity on the persistent entity side.
    private String description;
}
