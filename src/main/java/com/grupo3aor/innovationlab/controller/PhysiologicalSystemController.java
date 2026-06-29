package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.audit.AuditableAction;
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
    @AuditableAction(action = "CREATE_SYSTEM")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createSystem(
            @Valid @RequestBody PhysiologicalSystemRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String operatorEmail = authentication.getName();
        String originIp = httpRequest.getRemoteAddr();

        PhysiologicalSystemResponse response = systemService.createSystem(request, operatorEmail, originIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
    @AuditableAction(action = "DELETE_SYSTEM")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteSystem(@PathVariable Long id) {
        systemService.deleteSystem(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a single system by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PhysiologicalSystemResponse> getSystemById(@PathVariable Long id) {
        return ResponseEntity.ok(systemService.getSystemById(id));
    }

    /**
     * Updates an existing system configuration.
     */
    @PutMapping("/{id}")
    @AuditableAction(action = "UPDATE_SYSTEM")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PhysiologicalSystemResponse> updateSystem(
            @PathVariable Long id,
            @Valid @RequestBody PhysiologicalSystemRequest request) {
            
        PhysiologicalSystemResponse response = systemService.updateSystem(id, request);
        return ResponseEntity.ok(response);
    }
}
