package com.grupo3aor.innovationlab.dto;
import lombok.Getter;
import lombok.Setter;



// Usamos as anotações do Lombok para ele gerar os Getters e Setters
@Getter
@Setter
public class RegisterRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String confirmPassword;
    
    

}
