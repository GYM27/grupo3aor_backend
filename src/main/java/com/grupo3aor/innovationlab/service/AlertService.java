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

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AlertService {

    private final AlertRepository repository;
    private final SimulationMapper mapper;
    private final SimulationRepository simulationRepository;
    private final RuleRepository ruleRepository;

    public AlertService(AlertRepository repository, SimulationMapper mapper,
                        SimulationRepository simulationRepository, RuleRepository ruleRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.simulationRepository = simulationRepository;
        this.ruleRepository = ruleRepository;
    }

    @Transactional
    public AlertDTO triggerAlert(AlertDTO dto, String userEmail, String ipAddress) {
        // I resolve the Simulation and Rule entities first to establish proper FK relations.
        // This replaces the old approach of setting raw UUIDs, which had no referential integrity.
        Simulation simulation = simulationRepository.findById(dto.getSimulationId())
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + dto.getSimulationId()));

        Rule rule = ruleRepository.findById(dto.getRuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found with ID: " + dto.getRuleId()));

        log.info("Manual alert triggered for rule ID: {} in simulation ID: {}", rule.getId(), simulation.getId());

        Alert alert = mapper.toEntity(dto, simulation, rule);

        alert.setCreatedBy(userEmail);
        alert.setUpdatedBy(userEmail);
        alert.setOriginIp(ipAddress);

        return mapper.toDto(repository.save(alert));
    }

    @Transactional
    public AlertDTO updateStatus(UUID alertId, AlertStatus status, String userEmail) {
        Alert alert = repository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Target system alert context not found"));

        log.info("Alert {} status updated to {} by {}", alertId, status, userEmail);

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
