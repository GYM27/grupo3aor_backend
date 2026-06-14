package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.service.PhysiologicalReadingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/readings")
@PreAuthorize("isAuthenticated()")
public class PhysiologicalReadingController {

    private final PhysiologicalReadingService service;

    public PhysiologicalReadingController(PhysiologicalReadingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PhysiologicalReadingDTO> postReading(
            @Valid @RequestBody PhysiologicalReadingDTO dto,
            Authentication authentication,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.createReading(dto, authentication.getName(), request.getRemoteAddr()));
    }

    @PostMapping("/stream")
    public ResponseEntity<PhysiologicalReadingDTO> postReadingStream(
            @Valid @RequestBody PhysiologicalReadingDTO dto,
            Authentication authentication,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.createReading(dto, authentication.getName(), request.getRemoteAddr()));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<PhysiologicalReadingDTO>> postReadingBatch(
            @Valid @RequestBody List<PhysiologicalReadingDTO> dtos,
            Authentication authentication,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.createReadingBatch(dtos, authentication.getName(), request.getRemoteAddr()));
    }

    @GetMapping("/simulation/{simulationId}")
    public ResponseEntity<List<PhysiologicalReadingDTO>> getBySimulation(@PathVariable UUID simulationId) {
        return ResponseEntity.ok(service.getReadingsBySimulation(simulationId));
    }
}