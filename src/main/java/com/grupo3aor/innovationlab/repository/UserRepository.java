package com.grupo3aor.innovationlab.repository;

import com.grupo3aor.innovationlab.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Interface created to manage data access and persistence of the User entity.
 * Extending JpaRepository delegates to Spring Data JPA the automatic generation
 * of all fundamental CRUD operations in the H2 database.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Naming convention-based query configured to find
     * a user by their email address.
     * * The return type is wrapped in an 'Optional' because the provided email
     * may not exist in the table, allowing safe handling of absent records
     * in the service layer and preventing involuntary null exceptions.
     *
     * @param email Email address used in the search.
     * @return An Optional containing the user, if found.
     */
    Optional<User> findByEmail(String email);

    /**
     * Searches for a user by their activation token.
     * Used during the email account confirmation process.
     *
     * @param token The token generated upon registration.
     * @return An Optional containing the user, if a user with that token exists.
     */
    Optional<User> findByActivationToken(String token);
    
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