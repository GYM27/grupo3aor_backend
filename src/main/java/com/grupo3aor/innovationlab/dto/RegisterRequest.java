package com.grupo3aor.innovationlab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Data payload containing incoming client inputs for user registration.
 * <p>
 * I embedded structural validation criteria directly into this payload object 
 * to guarantee strict filter boundaries right at our application entries.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "First name is mandatory")
    @Size(max = 75, message = "First name cannot exceed 75 characters")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    @Size(max = 75, message = "Last name cannot exceed 75 characters")
    private String lastName;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email structure")
    private String email;

    /**
     * Strong password validation pattern.
     */
    // I applied this regular expression validation rule to reject weak or common passwords.
    // It enforces at least 8 characters, one uppercase, one lowercase, one digit, and one special character.
    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
        message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    private String password;

    @NotBlank(message = "Password confirmation is mandatory")
    private String confirmPassword;
}
