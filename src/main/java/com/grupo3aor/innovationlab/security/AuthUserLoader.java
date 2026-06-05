package com.grupo3aor.innovationlab.security;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

/**
 * Class designed to load user data during the authentication process.
 * * Implements the UserDetailsService interface to establish a direct bridge
 * between the native Spring Security engine and our persistence in H2.
 * Configures this service so the Spring Boot ecosystem knows exactly how to locate
 * a user and validate their access permissions based on sessions.
 */
@Service
public class AuthUserLoader implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Dependency injection via constructor.
     * This is the safest and most recommended practice by the Spring Boot ecosystem,
     * ensuring component immutability and facilitating future unit tests.
     *
     * @param userRepository The repository used to interact with the users table.
     */
    public AuthUserLoader(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves user data from the database using the provided email.
     * * This method is automatically invoked by Spring Security during login.
     * If the record is successfully found, the data is converted from our model to the native 
     * Spring class, mapping access profiles directly into the application's security context.
     *
     * @param email The email address provided on the frontend login screen.
     * @return A UserDetails object populated with encrypted credentials and user permissions.
     * @throws UsernameNotFoundException Thrown if the email does not exist in the database, interrupting login.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        // Intercept the search and throw an explicit exception if the email does not exist in the table
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Map the user permission by converting our PerfilEnum into a GrantedAuthority.
        // Adds the prefix "ROLE_" to respect the naming standard required by Spring Security.
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getPerfil().name());

        // Return the native Spring Security user instance via Builder.
        // Configure the 'disabled' state based on our 'ativado' field, ensuring that 
        // users who have not validated their account are automatically blocked by the Spring engine.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(authority))
                .disabled(!user.isAccountActivated()) // Block login if accountActivated == false!
                .build();
    }
}
