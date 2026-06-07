package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.dto.SimulationRequest;
import com.grupo3aor.innovationlab.dto.SimulationResponse;
import com.grupo3aor.innovationlab.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller tracking the operational simulations.
 */
@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * Starts a simulation for a specific scenario.
     */
    @PostMapping("/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> startSimulation(@Valid @RequestBody SimulationRequest request, Authentication authentication) {
        try {
            String operatorEmail = authentication.getName();
            SimulationResponse response = simulationService.startSimulation(request, operatorEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Stops an ongoing simulation securely.
     */
    @PostMapping("/{id}/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> stopSimulation(@PathVariable UUID id) {
        try {
            SimulationResponse response = simulationService.stopSimulation(id);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            // Return conflict if trying to stop an already stopped simulation
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Fetches the simulation execution history.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SimulationResponse>> getHistory() {
        return ResponseEntity.ok(simulationService.getHistory());
    }

    /**
     * Cancels an ongoing simulation securely.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelSimulation(@PathVariable UUID id) {
        try {
            SimulationResponse response = simulationService.cancelSimulation(id);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
