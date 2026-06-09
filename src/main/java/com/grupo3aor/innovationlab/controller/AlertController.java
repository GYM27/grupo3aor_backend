package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.AlertStatus;
import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.dto.AlertDTO;
import com.grupo3aor.innovationlab.repository.UserRepository;
import com.grupo3aor.innovationlab.service.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService service;
    private final UserRepository userRepository;

    public AlertController(AlertService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<AlertDTO> createAlert(@Valid @RequestBody AlertDTO dto, HttpServletRequest request) {
        return ResponseEntity.ok(service.triggerAlert(dto, getCurrentAuthenticatedUser(), request.getRemoteAddr()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AlertDTO> resolveAlert(@PathVariable UUID id, @RequestParam AlertStatus status) {
        return ResponseEntity.ok(service.updateStatus(id, status, getCurrentAuthenticatedUser()));
    }

    @GetMapping("/simulation/{simulationId}")
    public ResponseEntity<List<AlertDTO>> getBySimulation(@PathVariable UUID simulationId) {
        return ResponseEntity.ok(service.getAlertsBySimulation(simulationId));
    }

    private Long getCurrentAuthenticatedUser() { // Corrected to return Long
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized: Current session context missing authenticated principal");
        }
        String sessionEmail = authentication.getName();
        return userRepository.findByEmail(sessionEmail)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Entity profile error for active user: " + sessionEmail));
    }
}