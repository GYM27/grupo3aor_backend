package com.grupo3aor.innovationlab.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.domain.entity.Simulation;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    private static class TrackerState {
        LocalDateTime firstBreach;
        LocalDateTime firstRecovery;
    }

    // Thread-safe map to avoid memory leaks and concurrency issues. Key: Simulation ID -> (Rule ID -> TrackerState)
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, TrackerState>> simulationTrackers = new ConcurrentHashMap<>();

    public void clearSimulationState(UUID simulationId) {
        if (simulationId != null) {
            simulationTrackers.remove(simulationId);
            log.info("Cleared state tracking for simulation {}", simulationId);
        }
    }

    private TrackerState getTracker(UUID simId, UUID ruleId) {
        return simulationTrackers
            .computeIfAbsent(simId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(ruleId, k -> new TrackerState());
    }

    @Transactional
    public void evaluateReading(PhysiologicalReading reading) throws Exception {
        evaluateReadingsBatch(java.util.List.of(reading));
    }

    @Transactional
    public void evaluateReadingsBatch(java.util.List<PhysiologicalReading> readings) {
        if (readings == null || readings.isEmpty()) return;
        
        java.util.List<Rule> activeRules = ruleRepository.findByActiveTrue();
        if (activeRules.isEmpty()) return;

        Simulation currentSim = readings.get(0).getSimulation();
        UUID simId = currentSim.getId();

        // In-memory cache to track which rules have already triggered an active alert for the simulation
        java.util.Set<UUID> activeAlertsCache = new java.util.HashSet<>();
        
        // Since we check the DB only once per batch, this makes 10,000 checks drop to 1.
        for (Rule rule : activeRules) {
            boolean exists = alertRepository.existsBySimulationAndRuleAndStatus(currentSim, rule, AlertStatus.ATIVO);
            if (exists) {
                activeAlertsCache.add(rule.getId());
            }
        }

        for (PhysiologicalReading reading : readings) {
            for (Rule rule : activeRules) {
                try {
                    // Check if this rule actually applies to this specific reading handle
                    if (!rule.isApplicableTo(reading.getHandle())) continue;

                    Double val = reading.getValue() != null ? reading.getValue().doubleValue() : null;
                    boolean isTriggered = rule.isTriggeredBy(reading.getHandle(), val);
                    boolean isResolved = rule.isResolvedBy(reading.getHandle(), val);
                    boolean isAlerting = activeAlertsCache.contains(rule.getId());

                    TrackerState tracker = getTracker(simId, rule.getId());

                    if (isTriggered) {
                        // Reset recovery since we are back in critical zone
                        tracker.firstRecovery = null;

                        if (!isAlerting) {
                            if (tracker.firstBreach == null) {
                                tracker.firstBreach = reading.getTimestamp();
                            }
                            long diffSeconds = ChronoUnit.SECONDS.between(tracker.firstBreach, reading.getTimestamp());
                            int requiredSeconds = rule.getActivationPersistence() != null ? rule.getActivationPersistence() : 0;

                            if (diffSeconds >= requiredSeconds) {
                                // Trigger Alert
                                log.info("Rule triggered: {} (ID: {}) for Simulation: {}", rule.getName(), rule.getId(), currentSim.getId());
                                Alert newAlert = Alert.builder()
                                    .simulation(currentSim)
                                    .rule(rule)
                                    .status(AlertStatus.ATIVO)
                                    .valueAtTrigger(val)
                                    .timestamp(reading.getTimestamp())
                                    .build();
                                
                                newAlert = alertRepository.save(newAlert);
                                broadcastAlert(newAlert, rule.getSeverity().name());
                                
                                activeAlertsCache.add(rule.getId());
                                tracker.firstBreach = null; // Reset breach tracker
                            }
                        }
                    } else if (isResolved && isAlerting) {
                        // We are alerting and the reading is now in the safe zone
                        tracker.firstBreach = null;

                        if (tracker.firstRecovery == null) {
                            tracker.firstRecovery = reading.getTimestamp();
                            
                            // It just crossed the threshold, send WARNING (Convalescence)
                            Alert activeAlert = alertRepository.findFirstBySimulationAndRuleAndStatusOrderByTimestampDesc(currentSim, rule, AlertStatus.ATIVO);
                            if (activeAlert != null) {
                                activeAlert.setWarningAt(reading.getTimestamp());
                                alertRepository.save(activeAlert);
                                broadcastAlert(activeAlert, "WARNING");
                            }
                        }

                        long diffSeconds = ChronoUnit.SECONDS.between(tracker.firstRecovery, reading.getTimestamp());
                        int requiredSeconds = rule.getResolutionPersistence() != null ? rule.getResolutionPersistence() : 0;

                        if (diffSeconds >= requiredSeconds) {
                            // Stabilized!
                            log.info("Alert resolved for Rule: {} (ID: {}) for Simulation: {}", rule.getName(), rule.getId(), currentSim.getId());
                            Alert activeAlert = alertRepository.findFirstBySimulationAndRuleAndStatusOrderByTimestampDesc(currentSim, rule, AlertStatus.ATIVO);
                            if (activeAlert != null) {
                                activeAlert.setStatus(AlertStatus.RESOLVIDO);
                                activeAlert.setResolvedAt(reading.getTimestamp());
                                alertRepository.save(activeAlert);
                                broadcastAlert(activeAlert, "NORMAL");
                            }
                            
                            activeAlertsCache.remove(rule.getId());
                            tracker.firstRecovery = null;
                        }
                    } else {
                        // In between thresholds (Hysteresis band) or neither alerting nor triggered.
                        // Reset both trackers to ensure strict persistence requirement.
                        tracker.firstBreach = null;
                        tracker.firstRecovery = null;
                    }
                } catch (Exception e) {
                    log.error("Failed to parse or evaluate YAML rule during batch: {}", rule.getId(), e);
                }
            }
        }
    }

    private void broadcastAlert(Alert alert, String overrideSeverity) {
        if (alert == null || alert.getSimulation() == null) return;
        String alertTopic = "/topic/simulations/" + alert.getSimulation().getId() + "/alerts";
        
        String ruleName = alert.getRule() != null ? alert.getRule().getName() : "";
        String analyticalJustification = alert.getRule() != null ? alert.getRule().getAnalyticalJustification() : "";
        String formattedValue = com.grupo3aor.innovationlab.util.ClinicalFormatter.formatClinicalMessage(alert);
        
        String eventTimestamp = alert.getTimestamp() != null ? alert.getTimestamp().toString() : "";
        if ("NORMAL".equals(overrideSeverity) && alert.getResolvedAt() != null) {
            eventTimestamp = alert.getResolvedAt().toString();
        } else if ("WARNING".equals(overrideSeverity) && alert.getWarningAt() != null) {
            eventTimestamp = alert.getWarningAt().toString();
        }

        java.util.Map<String, Object> alertPayload = java.util.Map.of(
            "alertId",        alert.getId() != null ? alert.getId().toString() : "",
            "ruleId",         alert.getRule() != null ? alert.getRule().getId().toString() : "",
            "simulationId",   alert.getSimulation().getId().toString(),
            "severity",       overrideSeverity,
            "systemName",     alert.getRule().getSystem() != null ? alert.getRule().getSystem().getSystemName() : "Unknown",
            "ruleName",       ruleName,
            "analyticalJustification", analyticalJustification != null ? analyticalJustification : "",
            "formattedValue", formattedValue,
            "valueAtTrigger", alert.getValueAtTrigger() != null ? alert.getValueAtTrigger() : "",
            "timestamp",      eventTimestamp
        );
        messagingTemplate.convertAndSend(alertTopic, alertPayload);
    }
}