package com.grupo3aor.innovationlab.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;

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
        
        for (Rule rule : ruleRepository.findAll()) {
            // A Entidade (Rule) toma a decisão de forma encapsulada (Rich Domain Model)
            boolean isTriggered = rule.isTriggeredBy(reading.getHandle(), reading.getValue());

            // Se a regra disparar, verifico se já existe um alerta ativo para não enviar spam para a BD
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