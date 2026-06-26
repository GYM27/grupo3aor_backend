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
    private final RuleCacheService ruleCacheService;
    private final DegradedModeBufferService bufferService;
    private final DataPersistenceComponent persistenceComponent;
    
    // Using YAMLMapper instead of ObjectMapper to parse the "miniDSL YAML"
    private final YAMLMapper yamlMapper = new YAMLMapper();

    // Cache for tracking persistence in live events. Key: simId_ruleId, Value: timestamp of first anomalous reading
    private final java.util.concurrent.ConcurrentHashMap<String, java.time.LocalDateTime> liveBreachTracker = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Evaluates a physiological reading against all active rules in the system.
     * If a rule's conditions are met, it automatically generates and broadcasts an Alert.
     *
     * @param reading The physiological reading to be evaluated
     * @throws Exception if there is an error evaluating the dynamic rule
     */
    @Transactional
    public void evaluateReading(PhysiologicalReading reading) throws Exception {
        
        for (Rule rule : ruleCacheService.getActiveRules()) {
            try {
                // Check if this rule actually applies to this specific reading handle (e.g. HR vs SpO2)
                boolean matches = rule.isApplicableTo(reading.getHandle());

                // If the reading doesn't belong to this rule, ignore it completely to avoid wiping persistence.
                if (!matches) continue;

                // The Entity (Rule) makes the decision in an encapsulated manner (Rich Domain Model)
                boolean isTriggered = rule.isTriggeredBy(reading.getHandle(), reading.getValue() != null ? reading.getValue().doubleValue() : null);
                String trackingKey = reading.getSimulation().getId().toString() + "_" + rule.getId().toString();

                if (isTriggered) {
                    java.time.LocalDateTime firstBreach = liveBreachTracker.get(trackingKey);
                    if (firstBreach == null) {
                        liveBreachTracker.put(trackingKey, reading.getTimestamp());
                        firstBreach = reading.getTimestamp();
                    }
                    
                    long diffSeconds = java.time.temporal.ChronoUnit.SECONDS.between(firstBreach, reading.getTimestamp());
                    int requiredSeconds = rule.getPersistence() != null ? rule.getPersistence() : 0;

                    // If the rule triggers, we verify if an active alert already exists to avoid spamming the DB
                    if (diffSeconds >= requiredSeconds) {
                        boolean alreadyAlerting = false;
                        if (!bufferService.isDegraded()) {
                            try {
                                alreadyAlerting = alertRepository.existsBySimulationAndRuleAndStatus(
                                    reading.getSimulation(), rule, AlertStatus.ATIVO
                                );
                            } catch (Exception e) {
                                bufferService.setDegraded(true); // Circuit breaker disparou na leitura
                            }
                        }

                        if (!alreadyAlerting) {
                                Alert newAlert = Alert.builder()
                                    .simulation(reading.getSimulation())
                                    .rule(rule)
                                    .status(AlertStatus.ATIVO)
                                    .valueAtTrigger(reading.getValue() != null ? reading.getValue().doubleValue() : null)
                                    .timestamp(reading.getTimestamp())
                                    .build();
                            
                            // Circuit Breaker para Alertas
                            if (bufferService.isDegraded()) {
                                bufferService.addPendingAlert(newAlert);
                            } else {
                                try {
                                    persistenceComponent.saveAlertSafely(newAlert);
                                } catch (Exception e) {
                                    bufferService.setDegraded(true);
                                    bufferService.addPendingAlert(newAlert);
                                }
                            }

                            // Publish to the simulation-specific topic so the Dashboard receives it.
                            // We use a safe Map instead of serializing the JPA entity directly
                            // to avoid LazyInitializationException on LAZY relations.
                            String alertTopic = "/topic/simulations/" + reading.getSimulation().getId() + "/alerts";
                            java.util.Map<String, Object> alertPayload = java.util.Map.of(
                                "id",             newAlert.getId() != null ? newAlert.getId().toString() : "",
                                "simulationId",   reading.getSimulation().getId().toString(),
                                "severity",       rule.getSeverity().name(),
                                "systemName",     rule.getSystem() != null ? rule.getSystem().getSystemName() : "Unknown",
                                "valueAtTrigger", reading.getValue(),
                                "timestamp",      reading.getTimestamp().toString(),
                                "expressionDsl",  rule.getExpressionDsl() != null ? rule.getExpressionDsl() : ""
                            );
                            messagingTemplate.convertAndSend(alertTopic, alertPayload);
                        }
                        // Reset the persistence tracker since we already triggered the critical alert
                        liveBreachTracker.remove(trackingKey);
                    }
                } else {
                    // Patient stabilized. Reset the persistence tracker immediately (strict medical rigidity).
                    liveBreachTracker.remove(trackingKey);
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
    @org.springframework.transaction.annotation.Transactional
    public void evaluateReadingsBatch(java.util.List<PhysiologicalReading> readings) {
        if (readings == null || readings.isEmpty()) return;
        
        java.util.List<Rule> activeRules = ruleCacheService.getActiveRules();
        if (activeRules.isEmpty()) return;

        // In-memory cache to track which rules have already triggered an active alert for the simulation
        java.util.Set<String> activeAlertsCache = new java.util.HashSet<>();
        // In-memory cache for persistence tracking during this batch (Timestamp of first breach)
        java.util.Map<String, java.time.LocalDateTime> batchBreachTracker = new java.util.HashMap<>();
        
        com.grupo3aor.innovationlab.domain.entity.Simulation currentSim = readings.get(0).getSimulation();
        // Since we check the DB only once per simulation, this makes 10,000 checks drop to 1.
        for (Rule rule : activeRules) {
            if (!bufferService.isDegraded()) {
                try {
                    boolean exists = alertRepository.existsBySimulationAndRuleAndStatus(currentSim, rule, AlertStatus.ATIVO);
                    if (exists) {
                        activeAlertsCache.add(currentSim.getId().toString() + "_" + rule.getId().toString());
                    }
                } catch (Exception e) {
                    bufferService.setDegraded(true);
                }
            }
        }

        for (PhysiologicalReading reading : readings) {
            for (Rule rule : activeRules) {
                try {
                    // Check if this rule actually applies to this specific reading handle (e.g. HR vs SpO2)
                    boolean matches = rule.isApplicableTo(reading.getHandle());

                    // If the reading doesn't belong to this rule, ignore it completely to avoid wiping persistence.
                    if (!matches) continue;

                    boolean isTriggered = rule.isTriggeredBy(reading.getHandle(), reading.getValue() != null ? reading.getValue().doubleValue() : null);
                    String cacheKey = reading.getSimulation().getId().toString() + "_" + rule.getId().toString();

                    if (isTriggered) {
                        java.time.LocalDateTime firstBreach = batchBreachTracker.get(cacheKey);
                        if (firstBreach == null) {
                            batchBreachTracker.put(cacheKey, reading.getTimestamp());
                            firstBreach = reading.getTimestamp();
                        }
                        
                        long diffSeconds = java.time.temporal.ChronoUnit.SECONDS.between(firstBreach, reading.getTimestamp());
                        int requiredSeconds = rule.getPersistence() != null ? rule.getPersistence() : 0;

                        if (diffSeconds >= requiredSeconds) {
                            if (!activeAlertsCache.contains(cacheKey)) {
                                Alert newAlert = Alert.builder()
                                    .simulation(reading.getSimulation())
                                    .rule(rule)
                                    .status(AlertStatus.ATIVO)
                                    .valueAtTrigger(reading.getValue() != null ? reading.getValue().doubleValue() : null)
                                    .timestamp(reading.getTimestamp())
                                    .build();
                                
                                // To eliminate the frontend delay, we save and broadcast the alert IMMEDIATELY!
                                if (bufferService.isDegraded()) {
                                    bufferService.addPendingAlert(newAlert);
                                } else {
                                    try {
                                        newAlert = persistenceComponent.saveAlertSafely(newAlert);
                                    } catch (Exception e) {
                                        bufferService.setDegraded(true);
                                        bufferService.addPendingAlert(newAlert);
                                    }
                                }
                                
                                String alertTopic = "/topic/simulations/" + currentSim.getId() + "/alerts";
                                java.util.Map<String, Object> alertPayload = java.util.Map.of(
                                    "id",             newAlert.getId() != null ? newAlert.getId().toString() : "",
                                    "simulationId",   currentSim.getId().toString(),
                                    "severity",       newAlert.getRule().getSeverity().name(),
                                    "systemName",     newAlert.getRule().getSystem() != null ? newAlert.getRule().getSystem().getSystemName() : "Unknown",
                                    "valueAtTrigger", newAlert.getValueAtTrigger(),
                                    "timestamp",      newAlert.getTimestamp().toString(),
                                    "expressionDsl",  newAlert.getRule().getExpressionDsl() != null ? newAlert.getRule().getExpressionDsl() : ""
                                );
                                messagingTemplate.convertAndSend(alertTopic, alertPayload);

                                activeAlertsCache.add(cacheKey); // Mark as already alerting in cache
                            }
                            batchBreachTracker.remove(cacheKey);
                        }
                    } else {
                        // Patient stabilized. Reset the persistence tracker immediately.
                        batchBreachTracker.remove(cacheKey);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse or evaluate YAML rule during batch: {}", rule.getId(), e);
                }
            }
        }
    }
}