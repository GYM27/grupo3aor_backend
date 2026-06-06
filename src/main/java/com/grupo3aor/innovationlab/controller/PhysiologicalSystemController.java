package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.dto.PhysiologicalSystemRequest;
import com.grupo3aor.innovationlab.dto.PhysiologicalSystemResponse;
import com.grupo3aor.innovationlab.service.PhysiologicalSystemService;
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
 * REST boundary endpoint exposing operations for Physiological Systems.
 * <p>
 * I architected this controller to act as a strict perimeter gateway, authorizing requests
 * via Spring Security interceptors and delegating heavy processing to the underlying services.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@RestController
@RequestMapping("/api/physiological-systems")
@RequiredArgsConstructor
public class PhysiologicalSystemController {

    private final PhysiologicalSystemService systemService;

    /**
     * Protected endpoint generating new system entries.
     */
    // Based on the functional requirements, I restricted configuration endpoints strictly 
    // to the ADMIN profile. Users or Managers cannot alter these core global setups.
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createSystem(
            @Valid @RequestBody PhysiologicalSystemRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        try {
            // I captured the active operator's identity and network origin dynamically 
            // from the security context to ensure tamper-proof auditing logs.
            String operatorEmail = authentication.getName();
            String originIp = httpRequest.getRemoteAddr();

            PhysiologicalSystemResponse response = systemService.createSystem(request, operatorEmail, originIp);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // I caught the business conflict explicitly to return a proper 400 Bad Request status.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Publicly authenticated endpoint serving the list of available systems.
     */
    // I allowed any authenticated user to retrieve active systems since both Managers 
    // and Standard Users need this configuration data to execute simulations.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PhysiologicalSystemResponse>> getAllActiveSystems() {
        return ResponseEntity.ok(systemService.getAllActiveSystems());
    }

    /**
     * Protected endpoint recycling system entries.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteSystem(@PathVariable Long id, Authentication authentication) {
        try {
            String operatorEmail = authentication.getName();
            systemService.deleteSystem(id, operatorEmail);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
