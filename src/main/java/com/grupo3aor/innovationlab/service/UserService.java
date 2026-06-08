package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import com.grupo3aor.innovationlab.dto.UpdateUserRequest;
import com.grupo3aor.innovationlab.dto.UserResponse;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic tier managing administrative User operations.
 * <p>
 * I created this service to isolate the user management logic from the authentication flow,
 * providing the ADMIN with the capability to list, update, and soft-delete user accounts securely.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves all operational user records.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllActiveUsers() {
        return userRepository.findAllByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves exclusively logically deleted user records.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllInactiveUsers() {
        return userRepository.findAllByActiveFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Locates a single user based on their email address.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToResponse(user);
    }

    /**
     * Updates the role profile of an existing user.
     */
    @Transactional
    public UserResponse updateUserRole(String email, UpdateUserRequest request, String adminEmail) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Let's prevent the admin from changing their own profile to avoid accidental lockouts
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new IllegalStateException("Admins cannot change their own profile role.");
        }

        user.setPerfil(request.getPerfil());
        user.setUpdatedBy(adminEmail);
        
        User savedUser = userRepository.save(user);

        log.info("[AUDIT] Action: USER_ROLE_UPDATED | Target Email: {} | New Role: {} | Operator: {}", 
                 user.getEmail(), request.getPerfil(), adminEmail);

        return mapToResponse(savedUser);
    }

    /**
     * Soft deletes a user account.
     */
    @Transactional
    public void softDeleteUser(Long id, String adminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Prevent self-deletion
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new IllegalStateException("Admins cannot delete their own account.");
        }

        userRepository.delete(user);

        log.info("[AUDIT] Action: USER_SOFT_DELETED | Target ID: {} | Target Email: {} | Operator: {}", 
                 id, user.getEmail(), adminEmail);
    }

    /**
     * Helper conversion mechanism.
     */
    private UserResponse mapToResponse(User entity) {
        return UserResponse.builder()
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .perfil(entity.getPerfil().name())
                .accountActivated(entity.isAccountActivated())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
