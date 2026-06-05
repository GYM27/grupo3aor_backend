package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.dto.LoginRequest;
import com.grupo3aor.innovationlab.dto.RegisterRequest;
import com.grupo3aor.innovationlab.repository.UserRepository;
import com.grupo3aor.innovationlab.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller to expose authentication endpoints to the client application.
 * * The purpose of this class is to act as the entry point for login and registration processes,
 * receiving data in JSON format via DTOs and forwarding them to the service layer.
 * CrossOrigin is explicitly configured to enable credential sharing, allowing
 * session cookies (JSESSIONID) traffic with our local frontend.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    /**
     * Dependency injection for security and business operations.
     * The Spring Security AuthenticationManager is included to delegate
     * the authentication cycle and HTTP session registration natively.
     */
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                          AuthService authService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Endpoint to process user authentication attempts.
     * * Utilizes the native Spring Security ecosystem by submitting a credentials token
     * to the AuthenticationManager. If credentials are correct, the user's session
     * is saved in the server context and the JSESSIONID cookie is generated and sent
     * automatically to the browser.
     *
     * @param request Object containing the submitted email and password.
     * @param httpRequest Servlet request object used to force session creation.
     * @return An HTTP response containing profile data on success, or the appropriate error.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        // Fetch the user by email to pre-validate account activation status
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        User user = userOptional.get();

        // Validate if the account has already been activated before proceeding with authentication
        if (!user.isAtivado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account not activated. Please check your email."));
        }

        try {
            // Create clear text authentication token with submitted credentials
            UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

            // Delegate to AuthenticationManager the task of validating password against AuthUserLoader
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // Deposit validated authentication in Spring Boot's global security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Explicitly force HTTP session creation or retrieval on the server
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // Return profile and name for UI construction
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "perfil", user.getPerfil().name(),
                    "nomeCompleto", user.getFirstName() + " " + user.getLastName()
            ));

        } catch (Exception e) {
            // If authentication manager fails (wrong password), catch exception and respond with 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    /**
     * Endpoint to delegate new account creation process.
     * * Business logic (duplicate email validation, password confirmation and encryption)
     * was isolated in AuthService to keep this controller clean and focused on HTTP traffic.
     *
     * @param request Object containing all data submitted in the registration form.
     * @return HTTP 200 response on success, or 400 error on validation failure.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.registarNovoUtilizador(request);
            return ResponseEntity.ok(Map.of("message", "Registration successful. Please check your email to activate the account."));
        } catch (RuntimeException e) {
            // Catch business exceptions thrown by AuthService and return them nicely
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint for account activation via email link.
     * @param token The activation token.
     * @return Success or error message.
     */
    @GetMapping("/ativar")
    public ResponseEntity<?> ativarConta(@RequestParam("token") String token) {
        try {
            authService.ativarConta(token);
            return ResponseEntity.ok(Map.of("message", "Account activated successfully. You can now login."));
        } catch (com.grupo3aor.innovationlab.service.AuthService.AccountAlreadyActivatedException e) {
            // Account was already activated (token already used): return 200 with friendly message
            return ResponseEntity.ok(Map.of("message", e.getMessage(), "alreadyActivated", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
