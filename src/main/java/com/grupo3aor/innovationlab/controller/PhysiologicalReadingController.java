package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.service.PhysiologicalReadingService;
import com.grupo3aor.innovationlab.service.BioGearsParserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/readings")
@PreAuthorize("isAuthenticated()")
public class PhysiologicalReadingController {

    private final PhysiologicalReadingService service;
    private final BioGearsParserService parserService;

    public PhysiologicalReadingController(PhysiologicalReadingService service, BioGearsParserService parserService) {
        this.service = service;
        this.parserService = parserService;
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

    @PostMapping(value = "/simulation/{simulationId}/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PhysiologicalReadingDTO>> uploadBioGearsBatch(
            @PathVariable UUID simulationId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            List<PhysiologicalReadingDTO> readings = parserService.parseCsv(file, simulationId);
            return ResponseEntity.ok(service.createReadingBatch(readings, authentication.getName(), request.getRemoteAddr()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse CSV file: " + e.getMessage());
        }
    }

    @GetMapping("/simulation/{simulationId}")
    public ResponseEntity<List<PhysiologicalReadingDTO>> getBySimulation(@PathVariable UUID simulationId) {
        return ResponseEntity.ok(service.getReadingsBySimulation(simulationId));
    }
}