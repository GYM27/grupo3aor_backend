package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.audit.AuditableAction;
import com.grupo3aor.innovationlab.dto.RuleRequest;
import com.grupo3aor.innovationlab.dto.RuleResponse;
import com.grupo3aor.innovationlab.service.RuleService;
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
 * REST controller for managing engine rules.
 * <p>
 * I added explicit role protection here so only authorized people can change the simulation engine.
 * </p>
 */
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    /**
     * Creates a new rule in the system.
     * We extract the email from the Authentication token to guarantee security.
     */
    @PostMapping
    @AuditableAction(action = "CREATE_RULE")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createRule(@Valid @RequestBody RuleRequest request, Authentication authentication) {
        String operatorEmail = authentication.getName();
        RuleResponse response = ruleService.createRule(request, operatorEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetches rules, optionally filtering by active state via query parameter.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RuleResponse>> getAllRules(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ruleService.getAllRules(active));
    }

    /**
     * Deactivates a rule.
     * As requested, this method purely acts as a logical deactivation (soft delete).
     * It maps to a DELETE request to keep standard REST semantics, but data is NOT destroyed!
     */
    @DeleteMapping("/{id}")
    @AuditableAction(action = "DEACTIVATE_RULE")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deactivateRule(@PathVariable UUID id) {
        ruleService.deactivateRule(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Re-activates a soft-deleted rule.
     */
    @PutMapping("/{id}/activate")
    @AuditableAction(action = "ACTIVATE_RULE")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> activateRule(@PathVariable UUID id) {
        ruleService.activateRule(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Updates an existing rule.
     */
    @PutMapping("/{id}")
    @AuditableAction(action = "UPDATE_RULE")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateRule(@PathVariable UUID id, @Valid @RequestBody RuleRequest request, Authentication authentication) {
        String operatorEmail = authentication.getName();
        RuleResponse response = ruleService.updateRule(id, request, operatorEmail);
        return ResponseEntity.ok(response);
    }
}
