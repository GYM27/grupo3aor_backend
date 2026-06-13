package com.grupo3aor.innovationlab.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.dto.RuleCondition;

import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
@RequiredArgsConstructor
public class RuleEvaluatorService {

    private final AlertRepository alertRepository;
    private final RuleRepository ruleRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void evaluateReading(PhysiologicalReading reading) throws JsonProcessingException {
        
        for (Rule rule : ruleRepository.findAllByActiveTrue()) {
            RuleCondition ruleCondition = objectMapper.readValue(rule.getExpressionDsl(), RuleCondition.class);
            
            // I first verify if the rule's metric matches the reading's handle (e.g. "HEART_RATE")
            if (ruleCondition.getMetric().equals(reading.getHandle())) {
                
                boolean isTriggered = false;

                // Then I dynamically evaluate the threshold condition using a switch statement
                switch (ruleCondition.getOperator()) {
                    case ">": isTriggered = reading.getValue().compareTo(ruleCondition.getThreshold()) > 0;
                        break;
                    case "<": isTriggered = reading.getValue().compareTo(ruleCondition.getThreshold()) < 0;
                        break;
                    case "==": isTriggered = reading.getValue().compareTo(ruleCondition.getThreshold()) == 0;
                        break;
                }

                // If the rule triggered, I check if an active alert ALREADY EXISTS to avoid spamming the database
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
                        
                        // NOVO: Emite o Alerta Crítico para o Dashboard imediatamente!
                        messagingTemplate.convertAndSend("/topic/alerts", newAlert);
                    }
                }
            }
        }
    }
}