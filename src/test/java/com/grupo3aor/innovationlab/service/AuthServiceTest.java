package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import com.grupo3aor.innovationlab.dto.LoginRequest;
import com.grupo3aor.innovationlab.dto.RegisterRequest;
import com.grupo3aor.innovationlab.dto.UserResponse;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private com.grupo3aor.innovationlab.repository.GlobalSettingsRepository globalSettingsRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .perfil(PerfilEnum.USER)
                .accountActivated(true)
                .active(true)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("Pass123!");
        registerRequest.setConfirmPassword("Pass123!");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Pass123!");
    }

    // --- REGISTER TESTS ---

    @Test
    void registerNewUser_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_new_pass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> authService.registerNewUser(registerRequest, "127.0.0.1"));

        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendConfirmationEmail(eq("new@example.com"), eq("New"), anyString());
    }

    @Test
    void registerNewUser_PasswordsDoNotMatch() {
        registerRequest.setConfirmPassword("DifferentPass!");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.registerNewUser(registerRequest, "127.0.0.1"));

        assertEquals("Passwords do not match. Please try again.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerNewUser_EmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.registerNewUser(registerRequest, "127.0.0.1"));

        assertEquals("This email is already registered in the system.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // --- ACTIVATE TESTS ---

    @Test
    void activateAccount_Success() {
        testUser.setAccountActivated(false);
        testUser.setActivationToken("valid_token");
        when(userRepository.findByActivationToken("valid_token")).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> authService.activateAccount("valid_token"));

        assertTrue(testUser.isAccountActivated());
        assertNull(testUser.getActivationToken());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void activateAccount_AlreadyActivatedOrInvalidToken() {
        when(userRepository.findByActivationToken("invalid_token")).thenReturn(Optional.empty());

        AuthService.AccountAlreadyActivatedException exception = assertThrows(AuthService.AccountAlreadyActivatedException.class, () ->
                authService.activateAccount("invalid_token"));

        assertEquals("Your account is already activated! You can login directly.", exception.getMessage());
    }

    // --- LOGIN TESTS ---

    @Test
    void authenticateUser_Success() {
        Authentication authentication = mock(Authentication.class);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        lenient().when(globalSettingsRepository.findById(1L)).thenReturn(Optional.of(new com.grupo3aor.innovationlab.domain.entity.GlobalSettings()));

        UserResponse response = authService.authenticateUser(loginRequest, "127.0.0.1");

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void authenticateUser_EmailNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () ->
                authService.authenticateUser(loginRequest, "127.0.0.1"));
    }

    @Test
    void authenticateUser_NotActivated() {
        testUser.setAccountActivated(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        assertThrows(DisabledException.class, () ->
                authService.authenticateUser(loginRequest, "127.0.0.1"));
    }

    @Test
    @DisplayName("authenticateUser: deve lançar BadCredentials se o utilizador existir na BD mas estiver apagado via Soft Delete (@SQLRestriction)")
    void authenticateUser_SoftDeletedUser() {
        // Devido ao @SQLRestriction("active = true") na entidade User, o JPA vai 
        // devolver Optional.empty() para utilizadores apagados logicamente.
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () ->
                authService.authenticateUser(loginRequest, "127.0.0.1"));
    }

    // --- FORGOT PASSWORD TESTS ---

    @Test
    void forgotPassword_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> authService.forgotPassword("test@example.com"));

        assertNotNull(testUser.getResetPasswordToken());
        assertNotNull(testUser.getResetPasswordExpiresAt());
        verify(userRepository, times(1)).save(testUser);
        verify(emailService, times(1)).sendPasswordResetEmail(eq("test@example.com"), eq("Test"), anyString());
    }

    @Test
    void forgotPassword_EmailNotFound_ShouldNotThrow() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Anti-enumeration test: It should not throw any exception
        assertDoesNotThrow(() -> authService.forgotPassword("unknown@example.com"));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("forgotPassword: deve ignorar silenciosamente pedidos para emails com Soft Delete (Anti-Enumeration)")
    void forgotPassword_SoftDeletedUser_ShouldNotThrow() {
        // Se a conta sofreu Soft Delete, o repositório devolve vazio. 
        // O sistema finge que enviou para não revelar aos hackers que a conta existe mas está inativa.
        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.forgotPassword("deleted@example.com"));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    // --- RESET PASSWORD TESTS ---

    @Test
    void resetPassword_Success() {
        testUser.setResetPasswordToken("valid_reset_token");
        testUser.setResetPasswordExpiresAt(LocalDateTime.now().plusHours(1));
        when(userRepository.findByResetPasswordToken("valid_reset_token")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("new_hashed_password");

        assertDoesNotThrow(() -> authService.resetPassword("valid_reset_token", "NewPass123!"));

        assertNull(testUser.getResetPasswordToken());
        assertNull(testUser.getResetPasswordExpiresAt());
        assertEquals("new_hashed_password", testUser.getPasswordHash());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void resetPassword_InvalidToken() {
        when(userRepository.findByResetPasswordToken("invalid")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.resetPassword("invalid", "NewPass123!"));

        assertEquals("Invalid or missing password reset token.", exception.getMessage());
    }

    @Test
    void resetPassword_ExpiredToken() {
        testUser.setResetPasswordToken("expired_token");
        testUser.setResetPasswordExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired
        when(userRepository.findByResetPasswordToken("expired_token")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.resetPassword("expired_token", "NewPass123!"));

        assertEquals("Password reset token has expired.", exception.getMessage());
    }

    @Test
    void resetPassword_SameAsOldPassword() {
        testUser.setResetPasswordToken("valid_token");
        testUser.setResetPasswordExpiresAt(LocalDateTime.now().plusHours(1));
        when(userRepository.findByResetPasswordToken("valid_token")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPass123!", testUser.getPasswordHash())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.resetPassword("valid_token", "OldPass123!"));

        assertEquals("New password cannot be the same as the current password.", exception.getMessage());
    }
}
