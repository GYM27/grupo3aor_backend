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
     * Lists rules.
     */
    @GetMapping
    @AuditableAction(action = "LIST_RULES")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<RuleResponse>> getAllRules(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long systemId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "active,desc") String sort) {
            
        String[] sortParts = sort.split(",");
        org.springframework.data.domain.Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") ? 
            org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
            
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, sortParts[0]));
        
        return ResponseEntity.ok(ruleService.getAllRules(name, systemId, status, pageable));
    }

    /**
     * Soft-deletes a rule.
     * The Rule entity's @SQLDelete intercepts this and sets deleted=true.
     */
    @DeleteMapping("/{id}")
    @AuditableAction(action = "DELETE_RULE")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteRule(@PathVariable UUID id) {
        ruleService.deleteRule(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Deactivates a rule (sets active=false).
     */
    @PutMapping("/{id}/deactivate")
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
