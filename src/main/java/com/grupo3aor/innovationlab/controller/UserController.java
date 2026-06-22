package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.audit.AuditableAction;
import com.grupo3aor.innovationlab.dto.InviteUserRequest;
import com.grupo3aor.innovationlab.dto.UpdateUserRequest;
import com.grupo3aor.innovationlab.dto.UserResponse;
import com.grupo3aor.innovationlab.service.AuthService;
import com.grupo3aor.innovationlab.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService; // Precisamos do AuthService injetado

    /**
     * Lists all active users.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllActiveUsers( @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllActiveUsers(page, size));
    }

    /**
     * Lists all deactivated (soft-deleted) users.
     */
    @GetMapping("/archived")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllInactiveUsers(
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllInactiveUsers(page, size));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        return ResponseEntity.ok(userService.getUserStats());
    }

    /**
     * Lists all users, both active and inactive.
     */
    @GetMapping("/allUsers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    /**
     * Retrieves a single user by their email address.
     */
    @GetMapping("/{email}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/id/{id}/email")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> getUserEmailById(@PathVariable Long id) {
        String email = userService.getUserEmailById(id);
        return ResponseEntity.ok(Map.of("email", email));
    }

    /**
     * Updates a user's profile (Name, Surname, and Role).
     */
    @PutMapping("/{email}/role")
    @AuditableAction(action = "UPDATE_USER_ROLE")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable String email,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
            
        String adminEmail = authentication.getName();
        UserResponse response = userService.updateUser(email, request, adminEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates (soft-deletes) a user by setting their active status to false.
     */
    @DeleteMapping("/{email}")
    @AuditableAction(action = "SOFT_DELETE_USER")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> softDeleteUser(@PathVariable String email, Authentication authentication) {
        String adminEmail = authentication.getName();
        userService.softDeleteUser(email, adminEmail);
        return ResponseEntity.ok().build();
    }

    /**
     * Reactivates a deactivated user by setting their active status to true.
     */
    @PutMapping("/{email}/activate")
    @AuditableAction(action = "ACTIVATE_USER")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> activateUser(@PathVariable String email, Authentication authentication) {
        String adminEmail = authentication.getName();
        userService.activateUser(email, adminEmail);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/invite")
    @AuditableAction(action = "INVITE_USER")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> inviteUser(
            @Valid @RequestBody InviteUserRequest request, 
            HttpServletRequest httpRequest, 
            Authentication authentication) {
        try {
            String ipAddress = httpRequest.getRemoteAddr();
            String adminEmail = authentication.getName();
            authService.inviteUser(request.getEmail(), ipAddress, adminEmail);
            return ResponseEntity.ok(Map.of("message", "User invited successfully. A password reset email has been sent."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
