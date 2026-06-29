package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Inbound data payload capturing client credentials during authentication attempts.
 * <p>
 * I engineered this data transfer object to establish rigid structural boundaries right 
 * at our application entrance, intercepting malformed payloads before they reach our core services.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    /**
     * The email address acting as the unique user account identifier.
     */
    // I applied these strict constraints to make sure the incoming payload contains 
    // a syntactically valid email format, avoiding unnecessary database lookups for garbage inputs.
    @NotBlank(message = "Email address cannot be empty")
    @Email(message = "Invalid email address structure")
    private String email;

    /**
     * The plain-text password submitted by the client to be checked against the stored hash.
     */
    // I decided to enforce a size boundary layer directly on this inbound field. 
    // This choice protects our system against resource exhaustion or payload inflation attacks 
    // before the string is passed downstream to our password hashing utilities.
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    private String password;
}
