package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.repository.UserRepository;
import com.grupo3aor.innovationlab.service.PhysiologicalReadingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/readings")
public class PhysiologicalReadingController {

    private final PhysiologicalReadingService service;
    private final UserRepository userRepository;

    public PhysiologicalReadingController(PhysiologicalReadingService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<PhysiologicalReadingDTO> createReading(
            @Valid @RequestBody PhysiologicalReadingDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.createReading(dto, getCurrentAuthenticatedUser(), request.getRemoteAddr()));
    }

    @GetMapping("/simulation/{simulationId}")
    public ResponseEntity<List<PhysiologicalReadingDTO>> getBySimulation(@PathVariable UUID simulationId) {
        return ResponseEntity.ok(service.getReadingsBySimulation(simulationId));
    }

    private Long getCurrentAuthenticatedUser() { // Corrected to return Long
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized: Current session context missing authenticated principal");
        }
        String sessionEmail = authentication.getName();
        return userRepository.findByEmail(sessionEmail)
                .map(User::getId) // Now correctly mapped as a Long sequence
                .orElseThrow(() -> new RuntimeException("Entity profile error for active user: " + sessionEmail));
    }
}