package com.grupo3aor.innovationlab.dto;

import com.grupo3aor.innovationlab.domain.enums.Severity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * I created this DTO to securely send rule data back to the frontend.
 * Instead of sending the whole User object (which has the password hash!), 
 * I only send the creator's email so the frontend can display it safely.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleResponse {

    private UUID id;
    
    private String systemId;
    
    private String expressionDsl;
    
    private Severity severity;
    
    private String createdByUserEmail;
    
    private LocalDateTime createdAt;

}
