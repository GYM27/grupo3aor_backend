package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Inbound Data Transfer Object for creating or updating a Physiological System.
 * <p>
 * I engineered this strict payload container to prevent Over-Posting attacks,
 * ensuring that clients can only submit the exact structural data required 
 * for system creation, leaving auditing and security flags untouched.
 * </p>
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PhysiologicalSystemRequest {

    /**
     * The name of the physiological system.
     */
    // I applied the same boundary restrictions here as in the entity model to 
    // catch structural anomalies at the API entry point before they reach the database layer.
    @NotBlank(message = "System name cannot be empty")
    @Size(max = 100, message = "System name cannot exceed 100 characters")
    private String systemName;
}
