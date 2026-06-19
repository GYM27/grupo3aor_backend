package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import com.grupo3aor.innovationlab.dto.UpdateUserRequest;
import com.grupo3aor.innovationlab.dto.UserResponse;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User targetUser;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        targetUser = User.builder()
                .id(2L)
                .firstName("Target")
                .lastName("User")
                .email("target@example.com")
                .perfil(PerfilEnum.USER)
                .build();

        updateRequest = new UpdateUserRequest();
        updateRequest.setPerfil(PerfilEnum.MANAGER);
    }

    // --- UPDATE USER TESTS ---

    @Test
    void updateUser_Success() {
        when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        UserResponse response = userService.updateUser("target@example.com", updateRequest, "admin@example.com");

        assertNotNull(response);
        assertEquals(PerfilEnum.MANAGER.name(), response.getPerfil());
        assertEquals("admin@example.com", targetUser.getUpdatedBy());
        verify(userRepository, times(1)).save(targetUser);
    }

    @Test
    void updateUser_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                userService.updateUser("unknown@example.com", updateRequest, "admin@example.com"));

        assertEquals("User not found with email: unknown@example.com", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_PreventSelfUpdate() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(targetUser));
        targetUser.setEmail("admin@example.com"); // Simulate target is the admin

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                userService.updateUser("admin@example.com", updateRequest, "admin@example.com"));

        assertEquals("Admins cannot change their own profile role.", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    // --- SOFT DELETE TESTS ---

    @Test
    void softDeleteUser_Success() {
        when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));

        assertDoesNotThrow(() -> userService.softDeleteUser("target@example.com", "admin@example.com"));

        verify(userRepository, times(1)).delete(targetUser); // delete() triggers @SQLDelete
    }

    @Test
    void softDeleteUser_UserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                userService.softDeleteUser("unknown@example.com", "admin@example.com"));

        assertEquals("User not found with email: unknown@example.com", exception.getMessage());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void softDeleteUser_PreventSelfDelete() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(targetUser));
        targetUser.setEmail("admin@example.com"); // Target is the admin

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                userService.softDeleteUser("admin@example.com", "admin@example.com"));

        assertEquals("Admins cannot delete their own account.", exception.getMessage());
        verify(userRepository, never()).delete(any());
    }
}
