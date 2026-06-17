package com.grupo3aor.innovationlab.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleCondition {

    private String metric;
    private String operator;
    private BigDecimal threshold;
    
    // For composite conditions (AND/OR)
    private List<RuleCondition> conditions;

}