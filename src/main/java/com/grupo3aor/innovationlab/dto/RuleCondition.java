package com.grupo3aor.innovationlab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for representing a rule condition.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleCondition {

    private String metric;
    private String operator; // Re-used for activation logic
    
    // Degradation phase
    private Double activationThreshold;
    private Integer activationPersistence;
    
    // Recovery phase
    private Double resolutionThreshold;
    private Integer resolutionPersistence;
    
    private java.util.List<RuleCondition> conditions;

}