package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import com.grupo3aor.innovationlab.dto.RegisterRequest;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Main constructor mapping core dependencies required for safe authorization lifecycles.
     * * @param userRepository Core interface abstracting persistent storage engines.
     * @param passwordEncoder Cryptographic processing component handling irreversible hashing.
     * @param emailService Mail infrastructure module handling outbound notification links.
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        // I injected these modules to keep a clean separation between data access, 
        // hashing utilities, and outbound notification networks.
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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
                .perfil(PerfilEnum.UTILIZADOR) // Reverted to UTILIZADOR as per the current Enum
                .accountActivated(false) 
                .activationToken(token)
                .createdBy("SELF_REGISTRATION_FLOW")
                .originIp(ipAddress)
                .build();

        userRepository.save(newUser);

        // I delegated the notification logic to our mailing infrastructure, transmitting the 
        // token parameters safely to verify mailbox ownership.
        emailService.enviarEmailConfirmacao(newUser.getEmail(), newUser.getFirstName(), token);
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
}
