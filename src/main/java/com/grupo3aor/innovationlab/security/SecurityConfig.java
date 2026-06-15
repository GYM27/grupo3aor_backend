package com.grupo3aor.innovationlab.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * Configuration class for the application's security settings.
 *
 * Centralizes and manages all web security for the project.
 * Here we configure:
 *   - Public URLs (no login required)
 *   - Protected URLs (mandatory login)
 *   - The password encryption algorithm (BCrypt).
 *
 * NOTE: Currently in DEVELOPMENT mode.
 *       The H2 Console is left accessible for testing purposes.
 *       
 */
@Configuration      // Tells Spring: "Attention, this class contains configurations!"
@EnableWebSecurity  // Immediately enables Spring's main security system.
@EnableMethodSecurity // Activates @PreAuthorize annotations across the application.
public class SecurityConfig {

    /**
     * Defines the access rules for each URL of the application.
     *
     * This SecurityFilterChain guarantees that every incoming HTTP request 
     * goes through this chain and is evaluated before reaching our Controllers.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS globally
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // CSRF (Cross-Site Request Forgery) protection
            // Disabled for the H2 console and all API routes (/api/**), 
            // since REST APIs (and external medical devices sending data) do not use browser CSRF tokens.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/api/**")
            )

            // Configure X-Frame-Options to allow H2 Console to use iframes internally.
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )

            // Define access rules based on URLs
            .authorizeHttpRequests(auth -> auth
                // PUBLIC URLs (anyone can access without login)
                .requestMatchers("/h2-console/**").permitAll()     // Free access to the H2 console
                .requestMatchers("/api/auth/**").permitAll()        // Free access to Login and Registration endpoints
                .requestMatchers("/api/ws/**").permitAll()          // I explicitly permitted access to our WebSocket handshake to prevent Spring Security from throwing 500 errors.
                .requestMatchers("/actuator/health").permitAll()    // Free access to our Health check endpoint for monitoring
                .requestMatchers("/ws/**").permitAll()              // Free access to WebSockets

                // ALL other URLs not mentioned above will strictly require authentication!
                .anyRequest().authenticated()
            )

            // Temporarily enable Spring's default login form.
            // Will be removed later when our React frontend handles the login screen!
            .formLogin(form -> form.permitAll())

            // Finally, configure the logout process
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
                .permitAll()
            );

        return http.build();
    }

    /**
     * Formally defines that we will use BCrypt as our password encryption algorithm.
     *
     * Chosen because it's currently the industry's most robust standard for securely storing passwords in the database.
     * Being a one-way hash, it is literally impossible to reverse the hash and discover the original plain text password.
     * During authentication, BCrypt simply compares the stored hash with the hash of the login attempt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS Configuration to allow the Frontend (React/Vite) to communicate with the Backend.
     * Since the Frontend runs on port 5173 and the Backend on 8080, this is essential to prevent blocking.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allows requests from the frontend origin (Vite default port and fallback)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:5174"));
        // Allows essential HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allows sending headers in the request
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        // Allows sending credentials (cookies, authentication headers)
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Applies this CORS configuration to all backend endpoints
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Exposes the native Spring Security AuthenticationManager as a Bean.
     * This is mandatory because we inject this manager into our AuthController to
     * programmatically process logins and manually set the session.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
