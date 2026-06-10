package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.dto.LoginRequest;
import com.grupo3aor.innovationlab.dto.RegisterRequest;
import com.grupo3aor.innovationlab.dto.UserResponse;
import com.grupo3aor.innovationlab.repository.UserRepository;
import com.grupo3aor.innovationlab.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller exposing unified authentication and registration API endpoints.
 * <p>
 * I engineered this presentation layer component to manage structural HTTP payload traffic, 
 * handle cross-origin credential policies, extract incoming remote client network metadata,
 * and coordinate early authentication gatekeeping checks.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@RestController
@RequestMapping("/api/auth")
// I removed the @CrossOrigin annotation here because CORS is already configured globally
// in SecurityConfig.corsConfigurationSource() for all /** endpoints. Having it in both places
// is redundant and could cause conflicts if the allowed origins diverge in the future.
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public AuthController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * Intercepts incoming HTTP credentials and manages user session establishment.
     * * @param request Validated payload container capturing structural credentials inputs.
     * @param httpRequest Web servlet context used to capture incoming connection metadata.
     * @return HTTP response containing authorization summaries or contextual security errors.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();

        try {
            UserResponse responseBody = authService.authenticateUser(request, clientIp);

            // I explicitly forced session instantiation on the server memory bounds, 
            // making sure authentication data resides safely on the server side.
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            return ResponseEntity.ok(responseBody);

        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Intercepts payload data to delegate user account setup workflows.
     * * @param request Validated schema payload housing client registration parameters.
     * @param httpRequest Web servlet context used to capture incoming network coordinates.
     * @return Structured HTTP status message indicating process dispatch.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        try {
            // I isolated all operational transactional workflows (such as duplicate validation checks 
            // and security token computations) in our service module to keep our presentation layer clean.
            authService.registerNewUser(request, ipAddress);
            return ResponseEntity.ok(Map.of("message", "Registration successful. Please check your email to activate the account."));
        } catch (RuntimeException e) {
            log.error("[SECURITY_LOG] Action: REGISTRATION_REJECTED | Attempted Email: {} | Error: {} | Origin IP: {}", 
                    request.getEmail(), e.getMessage(), ipAddress);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Public API endpoint resolving email tokens to process account activations.
     * * @param token Cryptographically safe registration confirmation token key parameter.
     * @return Structured confirmation response mapping operation success or failure.
     */
    @GetMapping("/ativar")
    public ResponseEntity<?> ativarConta(@RequestParam("token") String token) {
        try {
            // I passed the token down to our business engine to process structural state alterations.
            authService.activateAccount(token);
            return ResponseEntity.ok(Map.of("message", "Account activated successfully. You can now login."));
        } catch (com.grupo3aor.innovationlab.service.AuthService.AccountAlreadyActivatedException e) {
            // I handled this specific exception path to return a friendly confirmation response 
            // if a client triggers an already consumed token or an already validated account.
            return ResponseEntity.ok(Map.of("message", e.getMessage(), "alreadyActivated", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
