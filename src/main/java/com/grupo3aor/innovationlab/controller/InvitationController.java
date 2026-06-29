package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.Invitation;
import com.grupo3aor.innovationlab.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class InvitationController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<Invitation>> getAllInvitations() {
        return ResponseEntity.ok(authService.getAllInvitations());
    }

    @PostMapping("/{email}/resend")
    public ResponseEntity<?> resendInvitation(@PathVariable String email, Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            authService.resendInvitation(email, adminEmail);
            return ResponseEntity.ok(java.util.Map.of("message", "Invitation resent successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteInvitation(@PathVariable String email) {
        authService.deleteInvitation(email);
        return ResponseEntity.ok().build();
    }
}
