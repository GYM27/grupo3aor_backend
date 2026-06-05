package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.dto.LoginRequest;
import com.grupo3aor.innovationlab.dto.RegisterRequest;
import com.grupo3aor.innovationlab.repository.UserRepository;
import com.grupo3aor.innovationlab.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    /**
     * Direct structural constructor injecting mandatory application modules.
     * * @param userRepository Core database layer interface managing persistence rows.
     * @param authService Core domain service layer handling business transaction validations.
     * @param authenticationManager Infrastructure subsystem manager running standard authentication tasks.
     */
    public AuthController(UserRepository userRepository, AuthService authService, 
                          AuthenticationManager authenticationManager) {
        // I injected these concrete boundaries to keep a rigorous distinction between 
        // raw row operations, persistent business transactions, and security workflows.
        this.userRepository = userRepository;
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Intercepts incoming HTTP credentials and manages user session establishment.
     * * @param request Validated payload container capturing structural credentials inputs.
     * @param httpRequest Web servlet context used to capture incoming connection metadata.
     * @return HTTP response containing authorization summaries or contextual security errors.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // I pulled the remote client address context immediately to log the exact 
        // network location attempting to gain access to our services.
        String clientIp = httpRequest.getRemoteAddr();

        // I decided to perform an early identity check against our persistence rows 
        // to handle account existence states before running any intensive hashing workloads.
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            log.warn("[SECURITY_ALERT] Action: FAILED_LOGIN_ATTEMPT | Target Email: {} | Reason: EMAIL_NOT_FOUND | Origin IP: {}", 
                    request.getEmail(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        User user = userOptional.get();

        // I integrated this strict activation gate to block unverified email profiles 
        // from utilizing application resources, enforcing account verification workflows up front.
        if (!user.isAccountActivated()) {
            log.warn("[SECURITY_ALERT] Action: FAILED_LOGIN_ATTEMPT | Target Email: {} | Reason: ACCOUNT_NOT_ACTIVATED | Origin IP: {}", 
                    request.getEmail(), clientIp);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account not activated. Please check your email."));
        }

        try {
            // I packaged the raw credentials token to submit it down to our validation subsystem.
            UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

            // I delegated the actual cryptographic hash comparison routines completely 
            // to our security ecosystem manager.
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // I deposited the validated token directly inside the global thread context 
            // memory storage to lock the client clearance state.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // I explicitly forced session instantiation on the server memory bounds, 
            // making sure authentication data resides safely on the server side.
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            log.info("[AUDIT] Action: SUCCESSFUL_LOGIN | Authenticated User: {} | Profile clearance: {} | Origin IP: {}", 
                    user.getEmail(), user.getPerfil().name(), clientIp);

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "perfil", user.getPerfil().name(),
                    "nomeCompleto", user.getFirstName() + " " + user.getLastName()
            ));

        } catch (Exception e) {
            // I set this generic trap block to catch validation anomalies or incorrect 
            // password attempts, responding with unified error formats to prevent username enumeration.
            log.warn("[SECURITY_ALERT] Action: FAILED_LOGIN_ATTEMPT | Target Email: {} | Reason: INVALID_CREDENTIALS | Origin IP: {}", 
                    request.getEmail(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
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
