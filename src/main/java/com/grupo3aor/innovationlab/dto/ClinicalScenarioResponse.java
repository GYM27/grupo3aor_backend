package com.grupo3aor.innovationlab.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Outbound Data Transfer Object representing a Clinical Scenario summary.
 * <p>
 * I implemented this response class to prevent accidental leakage of the 
 * underlying physical entity, exposing only the exact variables required by 
 * the frontend UI interfaces.
 * </p>
 * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClinicalScenarioResponse {

    /**
     * Public scenario identifier.
     */
    private Long id;

    /**
     * The title of the clinical scenario.
     */
    private String name;

    /**
     * Detailed medical description.
     */
    private String description;

    /**
     * Timestamp marking when the scenario was registered.
     */
    private LocalDateTime createdAt;

    /**
     * Operator identity who inserted the record.
     */
    private String createdBy;
}
