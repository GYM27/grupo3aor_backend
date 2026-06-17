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
    public void evaluateReading(PhysiologicalReading reading) throws JsonProcessingException {
        
        for (Rule rule : ruleRepository.findAll()) {
            try {
                // A Entidade (Rule) toma a decisão de forma encapsulada (Rich Domain Model)
            boolean isTriggered = rule.isTriggeredBy(reading.getHandle(), reading.getValue() != null ? reading.getValue().doubleValue() : null);

            // Se a regra disparar, verifico se já existe um alerta ativo para não enviar spam para a BD
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


}