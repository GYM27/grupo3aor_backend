package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Direct data access interface managing persistence routines for the {@link User} entity.
 * <p>
 * I leveraged Spring Data framework capabilities here to abstract infrastructure data access,
 * allowing our database operations to execute over standard embedded storage.
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Retrieves a user matching a specific email address.
     * * @param email Target identifier string.
     * @return An Optional wrapper surrounding the discovered User.
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Locates a user record based on an active email token.
     * * @param token Cryptographically safe registration token string.
     * @return An Optional entity wrapper.
     */
    Optional<User> findByActivationToken(String token);
    
    /**
     * Locates a user record based on a password reset token.
     * * @param token Cryptographically safe reset token string.
     * @return An Optional entity wrapper.
     */
    Optional<User> findByResetPasswordToken(String token);
    
    /**
     * Gathers all operational user records while filtering out logically removed entries.
     * * @return Collection of active system users.
     */
    // I introduced this distinct query method to bypass soft-deleted accounts during 
    // our everyday core business flows and authentication checks.
    List<User> findAllByActiveTrue();
    
    /**
     * Retrieves exclusively logically deleted user records.
     * * @return Collection of archived or deactivated records.
     */
    // I built this query specifically for our administrative views, enabling operators 
    // to inspect, audit, or completely restore previously removed accounts.
    List<User> findAllByActiveFalse();
    
    /**
     * Added this quick validation method to check the prior existence
     * of an email during the registration flow.
     * * This approach performs an optimized database check,
     * returning a simple boolean value. This avoids loading the entire
     * entity into server memory just to see if the email is taken.
     *
     * @param email Email address to be checked.
     * @return True if the email already exists in the table, false otherwise.
     */
    boolean existsByEmail(String email);
}