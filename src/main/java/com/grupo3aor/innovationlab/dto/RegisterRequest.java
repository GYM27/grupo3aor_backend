package com.grupo3aor.innovationlab.dto;
import lombok.Getter;
import lombok.Setter;



// Using Lombok annotations to generate Getters and Setters
@Getter
@Setter
public class RegisterRequest {

    private String firstName;
    private String lastName;
    private String email;

    @jakarta.validation.constraints.NotBlank(message = "Password is required")
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
        message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    private String password;
    
    @jakarta.validation.constraints.NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
    
    

}
