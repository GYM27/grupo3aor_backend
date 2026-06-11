package com.grupo3aor.innovationlab.dto;

import java.math.BigDecimal;
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

}