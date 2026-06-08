package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.dto.UpdateUserRequest;
import com.grupo3aor.innovationlab.dto.UserResponse;
import com.grupo3aor.innovationlab.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST boundary endpoint exposing operations for User management.
 * <p>
 * I architected this controller strictly for administrative tasks, allowing the ADMIN
 * profile to oversee, modify, and logically remove users from the platform.
 * </p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Lists all active users.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllActiveUsers() {
        return ResponseEntity.ok(userService.getAllActiveUsers());
    }

    /**
     * Lists all deactivated (soft-deleted) users.
     */
    @GetMapping("/archived")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllInactiveUsers() {
        return ResponseEntity.ok(userService.getAllInactiveUsers());
    }

    /**
     * Retrieves a single user by their email address.
     */
    @GetMapping("/{email}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    /**
     * Updates a user's role profile.
     */
    @PutMapping("/{email}/role")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable String email,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
            
        String adminEmail = authentication.getName();
        UserResponse response = userService.updateUserRole(email, request, adminEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Logically removes a user from the system.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id, Authentication authentication) {
        String adminEmail = authentication.getName();
        userService.softDeleteUser(id, adminEmail);
        return ResponseEntity.ok().build();
    }
}
