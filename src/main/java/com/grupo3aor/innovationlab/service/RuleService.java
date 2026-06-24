package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalSystem;
import com.grupo3aor.innovationlab.dto.RuleRequest;
import com.grupo3aor.innovationlab.dto.RuleResponse;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import com.grupo3aor.innovationlab.repository.UserRepository;
import com.grupo3aor.innovationlab.repository.PhysiologicalSystemRepository;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for managing rules.
 * <p>
 * I added this layer to keep the controllers clean. Here is where the DTO to Entity 
 * mapping happens, and where we inject the authenticated user into the new Rule!
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RuleService {

    private final RuleRepository ruleRepository;
    private final UserRepository userRepository;
    private final PhysiologicalSystemRepository systemRepository;

    /**
     * Creates a new rule, injecting the user securely based on their session email.
     */
    @Transactional
    public RuleResponse createRule(RuleRequest request, String userEmail) {
        // Fetch the user to ensure they actually exist before linking them
        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found in database!"));

        PhysiologicalSystem system = systemRepository.findById(request.getSystemId())
                .orElseThrow(() -> new ResourceNotFoundException("Physiological System not found with ID: " + request.getSystemId()));

        // Validation for Blood Pressure Limits
        validateBPLimits(request.getExpressionDsl());

        Rule rule = Rule.builder()
                .name(request.getName())
                .system(system)
                .expressionDsl(request.getExpressionDsl())
                .severity(request.getSeverity())
                .createdByUser(creator)
                .build();

        Rule saved = ruleRepository.save(rule);
        return mapToResponse(saved);
    }

    /**
     * Lists rules paginated and filtered.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RuleResponse> getAllRules(String name, Long systemId, String status, org.springframework.data.domain.Pageable pageable) {
        Boolean active = null;
        boolean deleted = false;

        if ("Ativa".equalsIgnoreCase(status)) {
            active = true;
        } else if ("Inativas".equalsIgnoreCase(status)) {
            active = false;
        } else if ("Eliminadas".equalsIgnoreCase(status)) {
            deleted = true;
        } // "Todas" ou outro valor mantém active = null e deleted = false

        org.springframework.data.domain.Page<Rule> rulesPage = ruleRepository.findFilteredRules(name, systemId, active, deleted, pageable);
        
        return rulesPage.map(this::mapToResponse);
    }

    /**
     * Soft-deletes a rule by ID. 
     * Because I added @SQLDelete in the Rule entity, this will just set deleted = true!
     */
    @Transactional
    public void deleteRule(UUID ruleId) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new ResourceNotFoundException("Rule not found with ID: " + ruleId);
        }
        ruleRepository.deleteById(ruleId);
    }

    /**
     * Deactivates a rule.
     */
    @Transactional
    public void deactivateRule(UUID ruleId) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found with ID: " + ruleId));
        rule.setActive(false);
        ruleRepository.save(rule);
    }

    /**
     * Re-activates a soft-deleted rule.
     */
    @Transactional
    public void activateRule(UUID ruleId) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found with ID: " + ruleId));
        rule.setActive(true);
        ruleRepository.save(rule);
    }

    /**
     * Updates an existing rule.
     */
    @Transactional
    public RuleResponse updateRule(UUID ruleId, RuleRequest request, String userEmail) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found with ID: " + ruleId));

        PhysiologicalSystem system = systemRepository.findById(request.getSystemId())
                .orElseThrow(() -> new ResourceNotFoundException("Physiological System not found with ID: " + request.getSystemId()));

        // Validation for Blood Pressure Limits
        validateBPLimits(request.getExpressionDsl());

        rule.setSystem(system);
        rule.setName(request.getName());
        rule.setExpressionDsl(request.getExpressionDsl());
        rule.setSeverity(request.getSeverity());
        // Fix: populate the audit field that was being silently ignored
        rule.setUpdatedBy(userEmail);

        log.info("[AUDIT] Action: RULE_UPDATED | Target ID: {} | Operator: {}", ruleId, userEmail);

        Rule saved = ruleRepository.save(rule);
        return mapToResponse(saved);
    }

    // =========================================================
    // HELPER MAPPERS
    // =========================================================
    private void validateBPLimits(String expressionDsl) {
        if (expressionDsl == null || expressionDsl.isEmpty()) return;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.grupo3aor.innovationlab.dto.RuleCondition condition = mapper.readValue(expressionDsl, com.grupo3aor.innovationlab.dto.RuleCondition.class);
            if ("BP".equalsIgnoreCase(condition.getMetric()) || "Pressão Arterial".equalsIgnoreCase(condition.getMetric())) {
                if (condition.getThreshold() < 0 || condition.getThreshold() > 300) {
                    throw new IllegalArgumentException("O valor limite para a Pressão Arterial deve estar entre 0 e 300 mmHg.");
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Failed to parse DSL for validation: {}", e.getMessage());
        }
    }

    private RuleResponse mapToResponse(Rule rule) {
        return RuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .systemId(rule.getSystem() != null ? rule.getSystem().getId() : null)
                .expressionDsl(rule.getExpressionDsl())
                .severity(rule.getSeverity())
                .createdByUserEmail(rule.getCreatedByUser() != null ? rule.getCreatedByUser().getEmail() : "Unknown")
                .createdAt(rule.getCreatedAt())
                .active(rule.isActive())
                .deleted(rule.isDeleted())
                .build();
    }
}
