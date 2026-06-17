package com.grupo3aor.innovationlab.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.dto.RuleCondition;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEvaluatorService {

    private final AlertRepository alertRepository;
    private final RuleRepository ruleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Using YAMLMapper instead of ObjectMapper to parse the "miniDSL YAML"
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @Transactional
    public void evaluateReading(PhysiologicalReading reading) {
        for (Rule rule : ruleRepository.findAllByActiveTrue()) {
            try {
                RuleCondition ruleCondition = yamlMapper.readValue(rule.getExpressionDsl(), RuleCondition.class);
                
                boolean isTriggered = evaluateCondition(ruleCondition, reading);

                // If the rule triggered, let's check if an active alert ALREADY EXISTS to avoid spamming the database
                if (isTriggered) {
                    boolean alreadyAlerting = alertRepository.existsBySimulationAndRuleAndStatus(
                        reading.getSimulation(), rule, AlertStatus.ATIVO
                    );

                    if (!alreadyAlerting) {
                        Alert newAlert = Alert.builder()
                            .simulation(reading.getSimulation())
                            .rule(rule)
                            .timestamp(reading.getTimestamp())
                            .status(AlertStatus.ATIVO)
                            .valueAtTrigger(reading.getValue())
                            .build();
                        
                        alertRepository.save(newAlert);

                        // Publish to the simulation-specific topic so the Dashboard receives it.
                        // We use a safe Map instead of serializing the JPA entity directly
                        // to avoid LazyInitializationException on LAZY relations.
                        String alertTopic = "/topic/simulations/" + reading.getSimulation().getId() + "/alerts";
                        java.util.Map<String, Object> alertPayload = java.util.Map.of(
                            "alertId",        newAlert.getId() != null ? newAlert.getId().toString() : "",
                            "simulationId",   reading.getSimulation().getId().toString(),
                            "severity",       rule.getSeverity().name(),
                            "systemName",     rule.getSystem() != null ? rule.getSystem().getSystemName() : "Unknown",
                            "valueAtTrigger", reading.getValue(),
                            "timestamp",      reading.getTimestamp().toString(),
                            "expressionDsl",  rule.getExpressionDsl() != null ? rule.getExpressionDsl() : ""
                        );
                        messagingTemplate.convertAndSend(alertTopic, alertPayload);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse or evaluate YAML rule: {}", rule.getId(), e);
            }
        }
    }

    private boolean evaluateCondition(RuleCondition condition, PhysiologicalReading reading) {
        // If it's a composite condition (e.g. AND / OR)
        if (condition.getConditions() != null && !condition.getConditions().isEmpty()) {
            if ("AND".equalsIgnoreCase(condition.getOperator())) {
                for (RuleCondition sub : condition.getConditions()) {
                    if (!evaluateCondition(sub, reading)) {
                        return false; // Fast fail
                    }
                }
                return true; // All matched
            } else if ("OR".equalsIgnoreCase(condition.getOperator())) {
                for (RuleCondition sub : condition.getConditions()) {
                    if (evaluateCondition(sub, reading)) {
                        return true; // Fast pass
                    }
                }
                return false; // None matched
            }
        } else {
            // It's a simple condition
            // Only evaluate if the metric matches the current reading
            if (!condition.getMetric().equals(reading.getHandle())) {
                return false; 
            }

            if (reading.getValue() == null || condition.getThreshold() == null) return false;

            // Dynamically evaluate the threshold condition
            switch (condition.getOperator()) {
                case ">": return reading.getValue().compareTo(condition.getThreshold()) > 0;
                case "<": return reading.getValue().compareTo(condition.getThreshold()) < 0;
                case "==": return reading.getValue().compareTo(condition.getThreshold()) == 0;
                case ">=": return reading.getValue().compareTo(condition.getThreshold()) >= 0;
                case "<=": return reading.getValue().compareTo(condition.getThreshold()) <= 0;
                default: return false;
            }
        }
        return false;
    }
}