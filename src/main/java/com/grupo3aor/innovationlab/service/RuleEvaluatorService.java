package com.grupo3aor.innovationlab.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.grupo3aor.innovationlab.dto.RuleCondition;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for dynamically evaluating physiological readings against the registered
 * clinical rules. Uses the Domain's rich model to interpret conditions and trigger alerts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEvaluatorService {

    private final AlertRepository alertRepository;
    private final RuleRepository ruleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Using YAMLMapper instead of ObjectMapper to parse the "miniDSL YAML"
    private final YAMLMapper yamlMapper = new YAMLMapper();

    /**
     * Evaluates a physiological reading against all active rules in the system.
     * If a rule's conditions are met, it automatically generates and broadcasts an Alert.
     *
     * @param reading The physiological reading to be evaluated
     * @throws Exception if there is an error evaluating the dynamic rule
     */
    @Transactional
    public void evaluateReading(PhysiologicalReading reading) throws Exception {
        
        for (Rule rule : ruleRepository.findByActiveTrue()) {
            try {
                // The Entity (Rule) makes the decision in an encapsulated manner (Rich Domain Model)
                boolean isTriggered = rule.isTriggeredBy(reading.getHandle(), reading.getValue() != null ? reading.getValue().doubleValue() : null);

                // If the rule triggers, we verify if an active alert already exists to avoid spamming the DB
                if (isTriggered) {
                    boolean alreadyAlerting = alertRepository.existsBySimulationAndRuleAndStatus(
                        reading.getSimulation(), rule, AlertStatus.ATIVO
                    );

                    if (!alreadyAlerting) {
                        Alert newAlert = Alert.builder()
                            .simulation(reading.getSimulation())
                            .rule(rule)
                            .status(AlertStatus.ATIVO)
                            .valueAtTrigger(reading.getValue() != null ? reading.getValue().doubleValue() : null)
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

    /**
     * Evaluates a batch of physiological readings optimally, minimizing database hits.
     * Prevents N+1 query problem by caching the active alert state in memory for the duration of the batch.
     *
     * @param readings The list of physiological readings to be evaluated
     */
    public void evaluateReadingsBatch(java.util.List<PhysiologicalReading> readings) {
        if (readings == null || readings.isEmpty()) return;
        
        java.util.List<Rule> activeRules = ruleRepository.findByActiveTrue();
        if (activeRules.isEmpty()) return;

        // In-memory cache to track which rules have already triggered an active alert for the simulation
        java.util.Set<String> activeAlertsCache = new java.util.HashSet<>();
        
        com.grupo3aor.innovationlab.domain.entity.Simulation currentSim = readings.get(0).getSimulation();
        // Since we check the DB only once per simulation, this makes 10,000 checks drop to 1.
        for (Rule rule : activeRules) {
            boolean exists = alertRepository.existsBySimulationAndRuleAndStatus(currentSim, rule, AlertStatus.ATIVO);
            if (exists) {
                activeAlertsCache.add(currentSim.getId().toString() + "_" + rule.getId().toString());
            }
        }

        java.util.List<Alert> newAlertsToSave = new java.util.ArrayList<>();

        for (PhysiologicalReading reading : readings) {
            for (Rule rule : activeRules) {
                try {
                    boolean isTriggered = rule.isTriggeredBy(reading.getHandle(), reading.getValue() != null ? reading.getValue().doubleValue() : null);

                    if (isTriggered) {
                        String cacheKey = reading.getSimulation().getId().toString() + "_" + rule.getId().toString();
                        if (!activeAlertsCache.contains(cacheKey)) {
                            Alert newAlert = Alert.builder()
                                .simulation(reading.getSimulation())
                                .rule(rule)
                                .status(AlertStatus.ATIVO)
                                .valueAtTrigger(reading.getValue() != null ? reading.getValue().doubleValue() : null)
                                .build();
                            
                            newAlertsToSave.add(newAlert);
                            activeAlertsCache.add(cacheKey); // Mark as already alerting in cache
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse or evaluate YAML rule during batch: {}", rule.getId(), e);
                }
            }
        }

        // Save all newly generated alerts in one batch
        if (!newAlertsToSave.isEmpty()) {
            alertRepository.saveAll(newAlertsToSave);
        }
    }
}