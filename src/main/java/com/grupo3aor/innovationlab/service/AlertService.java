package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.dto.AlertDTO;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AlertService {

    private final AlertRepository repository;
    private final SimulationMapper mapper;
    private final SimulationRepository simulationRepository;
    private final RuleRepository ruleRepository;

    @Autowired
    private DegradedModeBufferService bufferService;
    
    @Autowired
    private DataPersistenceComponent persistenceComponent;

    public AlertService(AlertRepository repository, SimulationMapper mapper,
                        SimulationRepository simulationRepository, RuleRepository ruleRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.simulationRepository = simulationRepository;
        this.ruleRepository = ruleRepository;
    }

    // Remove o @Transactional deste método se ele estiver a gerir a transação principal,
    // ou garante que as pesquisas (findById) são feitas antes de tentares guardar.
    public AlertDTO triggerAlert(AlertDTO dto, String userEmail, String ipAddress) {
        Simulation simulation = simulationRepository.findById(dto.getSimulationId())
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found..."));

        Rule rule = ruleRepository.findById(dto.getRuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found..."));

        Alert alert = mapper.toEntity(dto, simulation, rule);
        
        // Garantir que o ID não é nulo (embora o @Builder.Default na entidade já devesse tratar disto,
        // é uma boa prática de segurança caso o mapper não use o builder corretamente).
        if (alert.getId() == null) {
            alert.setId(UUID.randomUUID());
        }

        alert.setCreatedBy(userEmail);
        alert.setUpdatedBy(userEmail);
        alert.setOriginIp(ipAddress);

        // 1. O Circuit Breaker: Tentar gravar na Base de Dados
        if (bufferService.isDegraded()) {
            bufferService.addPendingAlert(alert);
            log.info("[MODO DEGRADADO] Alerta guardado no buffer em memória. ID: {}", alert.getId());
        } else {
            try {
                persistenceComponent.saveAlertSafely(alert);
            } catch (Exception e) {
                log.warn("[CIRCUIT BREAKER] Falha na BD ao guardar Alerta! A entrar em Modo Degradado.");
                bufferService.setDegraded(true);
                bufferService.addPendingAlert(alert);
            }
        }

        return mapper.toDto(alert);
    }

    @Transactional
    public AlertDTO updateStatus(UUID alertId, AlertStatus status, String userEmail) {
        Alert alert = repository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Target system alert context not found"));

        alert.setStatus(status);
        alert.setUpdatedBy(userEmail);

        return mapper.toDto(repository.save(alert));
    }

    @Transactional(readOnly = true)
    public List<AlertDTO> getAlertsBySimulation(UUID simulationId) {
        return repository.findBySimulation_Id(simulationId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
