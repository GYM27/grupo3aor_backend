package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for requesting a password reset.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    /**
     * The email address associated with the account.
     */
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be empty")
    private String email;

}
