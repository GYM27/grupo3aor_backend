package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.AlertStatus;
import com.grupo3aor.innovationlab.dto.AlertDTO;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final AlertRepository repository;
    private final SimulationMapper mapper;

    public AlertService(AlertRepository repository, SimulationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public AlertDTO triggerAlert(AlertDTO dto, Long userId, String ipAddress) {
        Alert alert = mapper.toEntity(dto);
        LocalDateTime now = LocalDateTime.now();

        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);
        alert.setCreatedBy(userId);
        alert.setUpdatedBy(userId);
        alert.setOriginIp(ipAddress);

        return mapper.toDto(repository.save(alert));
    }

    @Transactional
    public AlertDTO updateStatus(java.util.UUID alertId, AlertStatus status, Long userId) {
        Alert alert = repository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Target system alert context not found"));

        alert.setStatus(status);
        alert.setUpdatedAt(LocalDateTime.now());
        alert.setUpdatedBy(userId);

        return mapper.toDto(repository.save(alert));
    }

    @Transactional(readOnly = true)
    public List<AlertDTO> getAlertsBySimulation(java.util.UUID simulationId) {
        return repository.findBySimulationId(simulationId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}