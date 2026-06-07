package com.grupo3aor.innovationlab.dto;

import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * I created this DTO to return simulation data to the frontend dashboards.
 * Just like in the rules, I masked the User entity and only expose the email 
 * to guarantee no sensitive data leakage!
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationResponse {

    private UUID id;
    
    private Long scenarioId;
    
    private String userEmail;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime endedAt;
    
    private SimulationStatus status;

}
