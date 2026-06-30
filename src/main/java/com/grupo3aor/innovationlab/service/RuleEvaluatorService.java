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
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import jakarta.annotation.PostConstruct;

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
    private final MeterRegistry meterRegistry;
    
    // Adding these dependencies for degraded mode support
    private final DataPersistenceComponent dataPersistenceComponent;
    private final GlobalSettingsService globalSettingsService;

    // Adding this cache to serve as fallback when the database fails
    private List<Rule> cachedRules = new ArrayList<>();
    
    // Using YAMLMapper instead of ObjectMapper to parse the "miniDSL YAML"
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @PostConstruct
    public void initCache() {
        try {
            cachedRules = ruleRepository.findByActiveTrue();
            // Force Lazy Initialization of the system BEFORE the database fails!
            for (Rule rule : cachedRules) {
                if (rule.getSystem() != null) {
                    rule.getSystem().getSystemName();
                }
            }
            log.info("[DEGRADED MODE] Initialized RAM rule cache with {} rules on startup.", cachedRules.size());
        } catch (Exception e) {
            log.error("Failed to initialize rule cache on startup", e);
        }
    }

    private static class TrackerState {
        LocalDateTime firstBreach;
        LocalDateTime firstRecovery;
        // Adding this line to keep track of the active alert purely in memory
        Alert activeAlertMemory; 
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
        evaluateReadingsBatch(List.of(reading));
    }

    private List<Rule> getActiveRulesSafely() {
        try {
            if (globalSettingsService.isDbFailed()) {
                return cachedRules;
            }
            List<Rule> rules = ruleRepository.findByActiveTrue();
            // Force Lazy Initialization while the DB is still alive!
            for (Rule rule : rules) {
                if (rule.getSystem() != null) {
                    rule.getSystem().getSystemName();
                }
            }
            cachedRules = rules;
            return rules;
        } catch (Exception e) {
            log.warn("[DEGRADED MODE] Failed to read rules from the database. Activating degraded mode and using the memory cache.");
            globalSettingsService.setDbFailed(true);
            return cachedRules;
        }
    }

    @Transactional
    @Timed(value = "vitalsim.rule.evaluation.time", description = "Time taken to evaluate clinical rules")
    public void evaluateReadingsBatch(List<PhysiologicalReading> readings) {
        if (readings == null || readings.isEmpty()) return;

        // 1. Fetch active rules safely, falling back to RAM if the database is down
        List<Rule> activeRules = getActiveRulesSafely();
        if (activeRules.isEmpty()) return;

        Simulation currentSim = readings.get(0).getSimulation();
        UUID simId = currentSim.getId();

        for (PhysiologicalReading reading : readings) {
            for (Rule rule : activeRules) {
                try {
                    if (!rule.isApplicableTo(reading.getHandle())) continue;

                    Double val = reading.getValue() != null ? reading.getValue().doubleValue() : null;
                    boolean isTriggered = rule.isTriggeredBy(reading.getHandle(), val);
                    boolean isResolved = rule.isResolvedBy(reading.getHandle(), val);
                    
                    TrackerState tracker = getTracker(simId, rule.getId());
                    
                    // 2. Checking if the alert is active using strictly the RAM tracker
                    boolean isAlerting = (tracker.activeAlertMemory != null);

                    if (isTriggered) {
                        tracker.firstRecovery = null;

                        if (!isAlerting) {
                            if (tracker.firstBreach == null) {
                                tracker.firstBreach = reading.getTimestamp();
                            }

                            long diffSeconds = ChronoUnit.SECONDS.between(tracker.firstBreach, reading.getTimestamp());
                            int requiredSeconds = rule.getActivationPersistence() != null ? rule.getActivationPersistence() : 0;

                            if (diffSeconds >= requiredSeconds) {
                                log.info("Rule triggered: {} (ID: {}) for Simulation: {}", rule.getName(), rule.getId(), currentSim.getId());

                                Alert newAlert = Alert.builder()
                                    .id(UUID.randomUUID()) // Generating ID early so it exists in memory
                                    .simulation(currentSim)
                                    .rule(rule)
                                    .status(AlertStatus.ATIVO)
                                    .valueAtTrigger(val)
                                    .timestamp(reading.getTimestamp())
                                    .build();

                                // 3. Save with Circuit Breaker protection
                                try {
                                    if (globalSettingsService.isDbFailed()) {
                                        dataPersistenceComponent.queueAlert(newAlert);
                                    } else {
                                        newAlert = alertRepository.save(newAlert);
                                    }
                                } catch (Exception e) {
                                    log.warn("[DEGRADED MODE] Failed to save the new alert. Queuing it in memory instead.");
                                    globalSettingsService.setDbFailed(true);
                                    dataPersistenceComponent.queueAlert(newAlert);
                                }

                                // 4. Save the alert in the Tracker's RAM
                                tracker.activeAlertMemory = newAlert;
                                tracker.firstBreach = null;

                                broadcastAlert(newAlert, rule.getSeverity().name());
                                meterRegistry.counter("vitalsim.alerts.triggered").increment();
                            }
                        }
                    } else if (isResolved && isAlerting) {
                        tracker.firstBreach = null;

                        if (tracker.firstRecovery == null) {
                            tracker.firstRecovery = reading.getTimestamp();
                            
                            // Warning phase (Convalescence)
                            Alert activeAlert = tracker.activeAlertMemory;
                            activeAlert.setWarningAt(reading.getTimestamp());
                            
                            try {
                                if (globalSettingsService.isDbFailed()) {
                                    dataPersistenceComponent.queueAlert(activeAlert);
                                } else {
                                    alertRepository.save(activeAlert);
                                }
                            } catch (Exception e) {
                                globalSettingsService.setDbFailed(true);
                                dataPersistenceComponent.queueAlert(activeAlert);
                            }
                            
                            broadcastAlert(activeAlert, "WARNING");
                        }

                        long diffSeconds = ChronoUnit.SECONDS.between(tracker.firstRecovery, reading.getTimestamp());
                        int requiredSeconds = rule.getResolutionPersistence() != null ? rule.getResolutionPersistence() : 0;

                        if (diffSeconds >= requiredSeconds) {
                            log.info("Alert resolved for Rule: {} (ID: {}) for Simulation: {}", rule.getName(), rule.getId(), currentSim.getId());
                            
                            Alert activeAlert = tracker.activeAlertMemory;
                            activeAlert.setStatus(AlertStatus.RESOLVIDO);
                            activeAlert.setResolvedAt(reading.getTimestamp());
                            
                            try {
                                if (globalSettingsService.isDbFailed()) {
                                    dataPersistenceComponent.queueAlert(activeAlert);
                                } else {
                                    alertRepository.save(activeAlert);
                                }
                            } catch (Exception e) {
                                globalSettingsService.setDbFailed(true);
                                dataPersistenceComponent.queueAlert(activeAlert);
                            }

                            broadcastAlert(activeAlert, "NORMAL");
                            meterRegistry.counter("vitalsim.alerts.resolved").increment();

                            // 5. Clear the RAM tracker
                            tracker.activeAlertMemory = null;
                            tracker.firstRecovery = null;
                        }
                    } else {
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

        Map<String, Object> alertPayload = Map.of(
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