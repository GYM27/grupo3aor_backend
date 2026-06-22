package com.grupo3aor.innovationlab.controller;

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
     * Endpoint que recebe um ficheiro JSON e transforma-o num Cenário na BD.
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
            if (file.getSize() > 5 * 1024 * 1024) { // Limite de 5MB
                return ResponseEntity.badRequest().body("O ficheiro JSON é demasiado grande (limite: 5MB).");
            }

            // 1. A Magia do Jackson: Lê o input stream do ficheiro para o DTO
            ClinicalScenarioRequest request = objectMapper.readValue(file.getInputStream(), ClinicalScenarioRequest.class);

            // 2. Usa a lógica existente para criar na BD de forma segura
            String operatorEmail = authentication.getName();
            String originIp = httpRequest.getRemoteAddr();
            ClinicalScenarioResponse response = scenarioService.createScenario(request, operatorEmail, originIp);

            // 3. Retorna o Objeto criado (que já tem o ID da base de dados!)
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
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteScenario(@PathVariable Long id, Authentication authentication) {
        String operatorEmail = authentication.getName();
        scenarioService.deleteScenario(id, operatorEmail);
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
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ClinicalScenarioResponse> updateScenario(
            @PathVariable Long id,
            @Valid @RequestBody ClinicalScenarioRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
            
        String operatorEmail = authentication.getName();
        String originIp = httpRequest.getRemoteAddr();

        ClinicalScenarioResponse response = scenarioService.updateScenario(id, request, operatorEmail, originIp);
        return ResponseEntity.ok(response);
    }
}
