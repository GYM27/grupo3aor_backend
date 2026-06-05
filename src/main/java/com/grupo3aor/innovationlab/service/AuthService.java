package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import com.grupo3aor.innovationlab.dto.RegisterRequest;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for encapsulating the business logic associated with authentication.
 * 
 * Centralizes registration rules (such as validating duplicate emails and 
 * confirming passwords) and communicates with the repository to persist data.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Constructor for dependency injection.
     * Injects the encryption tool (PasswordEncoder), the database connection
     * (UserRepository) and the email service.
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Processes the registration request for a new user.
     * 
     * Executes the following validations:
     * 1. Checks if the passwords match.
     * 2. Ensures (via an optimized query) that the email is not already in use.
     * 3. Validates password strength.
     * 
     * Converts the DTO (RegisterRequest) into an Entity (User), encrypts the password 
     * and saves the record.
     *
     * @param request The object coming from the frontend with registration data (firstName, email, etc.)
     * @throws RuntimeException if any business rule is violated.
     */
    public void registarNovoUtilizador(RegisterRequest request) {
        
        // 1. Do the passwords match? (Initial security validation)
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match. Please try again.");
        }

        // Password strength validation
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$";
        if (!request.getPassword().matches(passwordRegex)) {
            throw new RuntimeException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character.");
        }

        // 2. Does the email already exist? We use the optimized search configured in UserRepository!
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("This email is already registered in the system.");
        }

        // Generate a unique activation token
        String token = UUID.randomUUID().toString();

        // 3. Translate the DTO into the Database Entity
        User novoUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                // Cybersecurity golden rule: NEVER store plain text passwords!
                .passwordHash(passwordEncoder.encode(request.getPassword())) 
                // By default, new users have basic permissions.
                .perfil(PerfilEnum.UTILIZADOR) 
                .ativado(false) 
                .activationToken(token)
                .build();

        // 4. Save permanently in H2 Database
        userRepository.save(novoUser);

        // 5. Send activation email
        emailService.enviarEmailConfirmacao(novoUser.getEmail(), novoUser.getFirstName(), token);
    }

    /**
     * Validates the token and activates the user account.
     * @param token The activation token received via email.
     */
    public void ativarConta(String token) {
        java.util.Optional<User> userOptional = userRepository.findByActivationToken(token);
        
        if (userOptional.isEmpty()) {
            // Check if there's any user with an already activated account (token already used)
            throw new AccountAlreadyActivatedException("Your account is already activated! You can login directly.");
        }
        
        User user = userOptional.get();
        user.setAtivado(true);
        user.setActivationToken(null);
        userRepository.save(user);
    }

    /**
     * Specific exception for when the account has already been activated previously.
     */
    public static class AccountAlreadyActivatedException extends RuntimeException {
        public AccountAlreadyActivatedException(String message) {
            super(message);
        }
    }
}
