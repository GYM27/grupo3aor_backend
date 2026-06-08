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
}
