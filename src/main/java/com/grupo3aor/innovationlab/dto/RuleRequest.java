package com.grupo3aor.innovationlab.dto;

import com.grupo3aor.innovationlab.domain.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * I created this DTO to securely receive new rule data from the frontend.
 * Notice that I completely omitted the "createdBy" field here. We will fetch the 
 * current user identity securely from the token session instead of trusting the frontend!
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleRequest {

    @NotBlank(message = "Rule name cannot be empty")
    private String name;

    @NotNull(message = "System ID cannot be empty")
    private Long systemId;

    @NotBlank(message = "Expression DSL cannot be empty")
    private String expressionDsl;

    @NotNull(message = "Severity must be provided (ALERTA or CRITICO)")
    private Severity severity;

}
