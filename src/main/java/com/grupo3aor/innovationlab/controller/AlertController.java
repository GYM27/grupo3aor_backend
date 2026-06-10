package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.AlertStatus;
import com.grupo3aor.innovationlab.dto.AlertDTO;
import com.grupo3aor.innovationlab.service.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@PreAuthorize("isAuthenticated()")
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AlertDTO> createAlert(@Valid @RequestBody AlertDTO dto, Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.ok(service.triggerAlert(dto, authentication.getName(), request.getRemoteAddr()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AlertDTO> resolveAlert(@PathVariable UUID id, @RequestParam AlertStatus status, Authentication authentication) {
        return ResponseEntity.ok(service.updateStatus(id, status, authentication.getName()));
    }

    @GetMapping("/simulation/{simulationId}")
    public ResponseEntity<List<AlertDTO>> getBySimulation(@PathVariable UUID simulationId) {
        return ResponseEntity.ok(service.getAlertsBySimulation(simulationId));
    }
}