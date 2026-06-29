package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for completing user registration after receiving an invitation.
 */
@Data
public class CompleteRegistrationRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

}
