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
     * Lists rules optionally filtered by their active state.
     */
    @Transactional(readOnly = true)
    public List<RuleResponse> getAllRules(Boolean active) {
        List<Rule> rules;
        if (active == null) {
            rules = ruleRepository.findAll();
        } else {
            rules = ruleRepository.findByActive(active);
        }
        
        return rules.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Soft-deletes a rule by ID. 
     * Because I added @SQLDelete in the Rule entity, this will just set active = false!
     */
    @Transactional
    public void deactivateRule(UUID ruleId) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new ResourceNotFoundException("Rule not found with ID: " + ruleId);
        }
        ruleRepository.deleteById(ruleId);
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
                .build();
    }
}
