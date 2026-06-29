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
 * Security infrastructure service loading user profile details during identity authentication.
 * <p>
 * I implemented this core interface to establish a transparent authentication bridge 
 * connecting our security subsystem to our persistence layer. This setup allows the runtime 
 * engine to locate profile configurations and load operational privileges during login requests.
 * </p>
 */
@Service
public class AuthUserLoader implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Direct structural constructor injecting our database abstraction layer.
     * * @param userRepository Core repository layer interface handling user records.
     */
    public AuthUserLoader(UserRepository userRepository) {
        // I opted for constructor injection here to ensure immutable component architecture, 
        // which helps avoid accidental side effects and simplifies unit testing setups.
        this.userRepository = userRepository;
    }

    /**
     * Locates a user record based on their unique email identifier string.
     * <p>
     * I structured this method to be invoked dynamically by the security manager during the login process.
     * If the profile is discovered, I convert our domain attributes into unified server security tokens,
     * embedding role clearance data straight into the application context.
     * </p>
     *
     * @param email The target identity email address submitted at the API entrance.
     * @return A secure UserDetails instance housing hashed credentials and role permissions.
     * @throws UsernameNotFoundException If the target email lookup fails inside our persistence rows.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        // I configured this query method to throw a clean exception if a profile lookup fails, 
        // halting unauthorized login execution before any resource allocation occurs.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // I removed the "ROLE_" prefix here. Our controllers use @PreAuthorize("hasAuthority('ADMIN')")
        // which looks for the exact string "ADMIN" without any prefix. This aligns the security 
        // context perfectly with our endpoint authorizations.
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getPerfil().name());

        // I constructed the outward-facing security user instance utilizing the native builder utility.
        // I explicitly mapped the account activation status to the disabled state property.
        // This design choice locks out unverified profiles at the gateway level, making sure 
        // users cannot log in before verifying mailbox ownership via their registration token.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(authority))
                .disabled(!user.isAccountActivated()) 
                .build();
    }
}
