package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import com.grupo3aor.innovationlab.dto.RegisterRequest;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.grupo3aor.innovationlab.dto.LoginRequest;
import com.grupo3aor.innovationlab.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service component orchestrating our core user authentication workflows.
 * <p>
 * I engineered this component to centralize identity management constraints, manage account
 * registration checks, handle secure email confirmation workflows, and record physical auditing lines.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;

    /**
     * Main constructor mapping core dependencies required for safe authorization lifecycles.
     * * @param userRepository Core interface abstracting persistent storage engines.
     * @param passwordEncoder Cryptographic processing component handling irreversible hashing.
     * @param emailService Mail infrastructure module handling outbound notification links.
     * @param authenticationManager Infrastructure subsystem manager running standard authentication tasks.
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Processes a registration payload, translating safe inputs into persistent system records.
     * <p>
     * I designed this business transaction flow to enforce strict relational entity mapping,
     * calculate unique tokens, and establish baseline security contexts.
     * </p>
     *
     * @param request Validated client payload mapping user registration details.
     * @param ipAddress Source physical network coordinates executing the operation.
     * @throws IllegalArgumentException If duplicate identities or internal mismatches occur.
     */
    @Transactional
    public void registerNewUser(RegisterRequest request, String ipAddress) {
        
        // I kept this specific comparison block in the business service to catch typos 
        // or data synchronization mismatches before touching the persistent resources.
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match. Please try again.");
        }

        // I skipped duplicate regex validation blocks here, electing to fully trust the 
        // automated structural validation constraints embedded in our inbound DTO.

        // I set this unique database query check to abort early if a client attempts 
        // to overwrite an email already managed by our infrastructure.
        // (Optimized using existsByEmail to prevent full entity loading)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("This email is already registered in the system.");
        }

        // I decided to generate a secure UUID token to act as an unforgeable registration 
        // validation key for our activation email pipeline.
        String token = UUID.randomUUID().toString();

        // I built this persistent record defaulting permissions to basic clearance states,
        // ensuring role elevation is strictly handled as an post-activation admin flow.
        User newUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword())) 
                .perfil(PerfilEnum.USER) // Automatically assigning the base tier role
                .accountActivated(false) 
                .activationToken(token)
                .createdBy("SELF_REGISTRATION_FLOW")
                .originIp(ipAddress)
                .build();

        userRepository.save(newUser);

        // I delegated the notification logic to our mailing infrastructure, transmitting the 
        // token parameters safely to verify mailbox ownership.
        emailService.sendConfirmationEmail(newUser.getEmail(), newUser.getFirstName(), token);
    }

    /**
     * Intercepts validation tokens and flips user structural records to an active state.
     * * @param token Unique cryptographically secure token string received by the client.
     * @throws AccountAlreadyActivatedException If the target token was already consumed or cleared.
     */
    @Transactional
    public void activateAccount(String token) {
        Optional<User> userOptional = userRepository.findByActivationToken(token);
        
        // I opted to throw a specialized runtime exception if the query comes back empty,
        // indicating that the link was either forged, tampered with, or previously consumed.
        if (userOptional.isEmpty()) {
            throw new AccountAlreadyActivatedException("Your account is already activated! You can login directly.");
        }
        
        User user = userOptional.get();
        
        // I chose to toggle our logical validation flag to true and wipe out the token field,
        // making sure that a confirmation link cannot be recycled maliciously.
        user.setAccountActivated(true);
        user.setActivationToken(null);
        
        // I manual-set these tracing metadata markers to track our activation state changes cleanly.
        user.setUpdatedBy(user.getEmail());
        
        userRepository.save(user);
    }

    /**
     * Domain runtime exception signaling account activation redundancies or token reuse.
     */
    public static class AccountAlreadyActivatedException extends RuntimeException {
        
        /**
         * Constructs the exception mapping an explicit explanation message.
         * * @param message Descriptive error tracking data.
         */
        public AccountAlreadyActivatedException(String message) {
            super(message);
        }
    }

    /**
     * Orchestrates the login workflow including validation, hashing and context setup.
     */
    @Transactional(readOnly = true)
    public UserResponse authenticateUser(LoginRequest request, String clientIp) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            log.warn("[SECURITY_ALERT] Action: FAILED_LOGIN_ATTEMPT | Target Email: {} | Reason: EMAIL_NOT_FOUND | Origin IP: {}", 
                    request.getEmail(), clientIp);
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = userOptional.get();

        if (!user.isAccountActivated()) {
            log.warn("[SECURITY_ALERT] Action: FAILED_LOGIN_ATTEMPT | Target Email: {} | Reason: ACCOUNT_NOT_ACTIVATED | Origin IP: {}", 
                    request.getEmail(), clientIp);
            throw new DisabledException("Account not activated. Please check your email.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("[AUDIT] Action: SUCCESSFUL_LOGIN | Authenticated User: {} | Profile clearance: {} | Origin IP: {}", 
                    user.getEmail(), user.getPerfil().name(), clientIp);
                    
            return UserResponse.builder()
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .perfil(user.getPerfil().name())
                    .accountActivated(user.isAccountActivated())
                    .createdAt(user.getCreatedAt())
                    .build();
        } catch (Exception e) {
            log.warn("[SECURITY_ALERT] Action: FAILED_LOGIN_ATTEMPT | Target Email: {} | Reason: INVALID_CREDENTIALS | Origin IP: {}", 
                    request.getEmail(), clientIp);
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    /**
     * Initiates the password recovery workflow by generating a secure token.
     * <p>
     * For security reasons against email enumeration, this method will simply log a warning
     * and exit silently if the email does not exist, pretending it succeeded.
     * </p>
     *
     * @param email The target email address for password recovery.
     */
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            // We do not throw an exception here to prevent attackers from guessing registered emails.
            log.warn("[SECURITY_ALERT] Action: FORGOT_PASSWORD_ATTEMPT | Target Email: {} | Reason: EMAIL_NOT_FOUND", email);
            return;
        }

        User user = userOptional.get();

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        // Token expires in 1 hour
        user.setResetPasswordExpiresAt(LocalDateTime.now().plusHours(1));
        
        user.setUpdatedBy("PASSWORD_RECOVERY_FLOW");
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
        log.info("[AUDIT] Action: PASSWORD_RECOVERY_REQUESTED | Target Email: {}", email);
    }

    /**
     * Resets the user's password using the secure token provided via email.
     *
     * @param token The secure reset token.
     * @param newPassword The new password in plain text.
     * @throws IllegalArgumentException If the token is invalid, expired, or matches the old password.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Optional<User> userOptional = userRepository.findByResetPasswordToken(token);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid or missing password reset token.");
        }

        User user = userOptional.get();

        if (user.getResetPasswordExpiresAt() == null || user.getResetPasswordExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired.");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiresAt(null);
        
        user.setUpdatedBy(user.getEmail());
        userRepository.save(user);
        
        log.info("[AUDIT] Action: PASSWORD_RESET_SUCCESSFUL | Target Email: {}", user.getEmail());
    }
}
