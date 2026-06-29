package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.audit.AuditableAction;
import com.grupo3aor.innovationlab.dto.ClinicalScenarioRequest;
import com.grupo3aor.innovationlab.dto.ClinicalScenarioResponse;
import com.grupo3aor.innovationlab.service.ClinicalScenarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * REST boundary endpoint exposing operations for Clinical Scenarios.
 * <p>
 * I designed this controller as a safe HTTP access point, locking administrative mutations 
 * behind strict role checks while exposing lookup queries to active simulator operators.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@RestController
@RequestMapping("/api/clinical-scenarios")
@RequiredArgsConstructor
public class ClinicalScenarioController {

    private final ClinicalScenarioService scenarioService;
    private final ObjectMapper objectMapper;


    /**
     * Protected endpoint generating new scenario entries.
     */
    @PostMapping
    @AuditableAction(action = "CREATE_SCENARIO")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createScenario(
            @Valid @RequestBody ClinicalScenarioRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String operatorEmail = authentication.getName();
        String originIp = httpRequest.getRemoteAddr();

        ClinicalScenarioResponse response = scenarioService.createScenario(request, operatorEmail, originIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint that receives a JSON file and transforms it into a Scenario in the DB.
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadScenario(
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        try {
            if (file.isEmpty() || !("application/json".equals(file.getContentType()))) {
                return ResponseEntity.badRequest().body("Ficheiro inválido. Apenas ficheiros JSON são permitidos.");
            }
            if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
                return ResponseEntity.badRequest().body("O ficheiro é demasiado grande (limite: 5MB).");
            }

            // 1. The Magic of Jackson: Reads the file input stream into the DTO
            ClinicalScenarioRequest request = objectMapper.readValue(file.getInputStream(), ClinicalScenarioRequest.class);

            // 2. Uses existing logic to create safely in the DB
            String operatorEmail = authentication.getName();
            String originIp = httpRequest.getRemoteAddr();
            ClinicalScenarioResponse response = scenarioService.createScenario(request, operatorEmail, originIp);

            // 3. Returns the created Object (which already has the database ID!)
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao processar o JSON: " + e.getMessage());
        }
    }


    /**
     * Publicly authenticated endpoint serving the list of available scenarios.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClinicalScenarioResponse>> getAllActiveScenarios() {
        return ResponseEntity.ok(scenarioService.getAllActiveScenarios());
    }

    /**
     * Protected endpoint recycling scenario entries.
     */
    @DeleteMapping("/{id}")
    @AuditableAction(action = "DELETE_SCENARIO")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteScenario(@PathVariable Long id) {
        scenarioService.deleteScenario(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a single scenario by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClinicalScenarioResponse> getScenarioById(@PathVariable Long id) {
        return ResponseEntity.ok(scenarioService.getScenarioById(id));
    }

    /**
     * Updates an existing scenario configuration.
     */
    @PutMapping("/{id}")
    @AuditableAction(action = "UPDATE_SCENARIO")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ClinicalScenarioResponse> updateScenario(
            @PathVariable Long id,
            @Valid @RequestBody ClinicalScenarioRequest request) {
            
        ClinicalScenarioResponse response = scenarioService.updateScenario(id, request);
        return ResponseEntity.ok(response);
    }
}
