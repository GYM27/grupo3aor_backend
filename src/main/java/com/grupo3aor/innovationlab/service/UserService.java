package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.enums.PerfilEnum;
import com.grupo3aor.innovationlab.dto.UpdateUserRequest;
import com.grupo3aor.innovationlab.dto.UserResponse;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     * Retrieves overall user statistics.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getUserStats() {
        long total = userRepository.countAllUsers();
        long inactive = userRepository.countInactiveUsers();
        long active = total - inactive;
        return java.util.Map.of(
            "totalUsers", total,
            "activeUsers", active,
            "inactiveUsers", inactive
        );
    }

    /**
     * Retrieves all users (active and inactive).
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAllUsers(pageable);
        return userPage.map(user -> mapToResponse(user));
    }

    /**
     * Retrieves all operational user records.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllActiveUsers( int page, int size) {
      Pageable pageable = PageRequest.of(page, size);
      Page<User> userPage = userRepository.findAllByActiveTrue(pageable);   
        
        return userPage.map(user -> mapToResponse(user));
    }

    /**
     * Retrieves exclusively logically deleted user records.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllInactiveUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAllByActiveFalse(pageable);
        return userPage.map(this::mapToResponse);
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

        @Transactional(readOnly = true)
    public String getUserEmailById(Long id) {
        return userRepository.findById(id)
                .map(User::getEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Transactional
    public UserResponse updateUser(String email, UpdateUserRequest request, String adminEmail) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.getEmail().equalsIgnoreCase(adminEmail) && request.getPerfil() != null && request.getPerfil() != user.getPerfil()) {
            throw new IllegalStateException("Admins cannot change their own profile role.");
        }

        if (request.getPerfil() != null) user.setPerfil(request.getPerfil());
        if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) user.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) user.setLastName(request.getLastName().trim());
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) user.setEmail(request.getEmail().trim());
        if (request.getActive() != null) user.setActive(request.getActive());

        user.setUpdatedBy(adminEmail);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public void softDeleteUser(String email, String adminEmail) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new IllegalStateException("Admins cannot delete their own account.");
        }

        userRepository.delete(user);
    }

    @Transactional
    public void activateUser(String email, String adminEmail) {
        int updatedCount = userRepository.activateUserByEmail(email);
        if (updatedCount == 0) {
            throw new ResourceNotFoundException("User not found or already active with email: " + email);
        }
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
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
