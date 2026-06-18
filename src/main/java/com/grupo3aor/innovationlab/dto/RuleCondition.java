package com.grupo3aor.innovationlab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleCondition {

    private String metric;
    private String operator;
    private Double threshold;
    private java.util.List<RuleCondition> conditions;

}