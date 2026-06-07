package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * I created this DTO for when the frontend wants to start a new Simulation.
 * I only ask for the Scenario ID. The backend will automatically handle the 
 * timestamps, the status (INICIADA), and the user identity securely.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationRequest {

    @NotNull(message = "Scenario ID cannot be empty")
    private Long scenarioId;

}
