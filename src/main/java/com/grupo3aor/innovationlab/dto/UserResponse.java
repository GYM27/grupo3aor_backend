package com.grupo3aor.innovationlab.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Safe outbound data transfer object representing unified user summaries.
 * <p>
 * I engineered this metadata container to act as an operational data firewall, 
 * ensuring that sensitive persistence fields, credential hashes, or internal 
 * security tokens are never exposed over our public network endpoints.
 * </p>
 * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    
    /**
     * Unique business identifier matching the user's registered identity.
     */
    // I opted to use the unique email string as the primary resource identifier 
    // for outward-facing client operations. This technical choice allows me to hide 
    // our sequential database primary keys, shielding the application from basic 
    // enumeration attacks or resource guessing vulnerabilities.
    private String email;
    
    /**
     * User's first name.
     */
    private String firstName;
    
    /**
     * User's last name.
     */
    private String lastName;
    
    /**
     * Operational security profile mapping assigned authorization clearance.
     */
    private String perfil;
    
    /**
     * Verification state tracking the user's account confirmation workflow.
     */
    private boolean accountActivated;

    /**
     * Operational state indicating if the user is currently active or soft-deleted.
     */
    private boolean active;

    /**
     * The system's session timeout configuration in minutes.
     */
    private Integer sessionTimeoutMinutes;

    /**
     * Timestamp marking when the user profile was originally registered.
     */
    // I decided to include this specific creation timestamp in the outbound payload. 
    // While I intentionally filtered out network-level auditing data like source IPs, 
    // providing this date allows our presentation layer to safely display registration 
    // history milestones on the client interface.
    private LocalDateTime createdAt;
}
