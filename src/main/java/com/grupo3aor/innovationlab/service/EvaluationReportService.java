package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.EvaluationReport;
import com.grupo3aor.innovationlab.dto.EvaluationReportDTO;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.EvaluationReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class EvaluationReportService {

    private final EvaluationReportRepository repository;
    private final SimulationMapper mapper;

    public EvaluationReportService(EvaluationReportRepository repository, SimulationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public EvaluationReportDTO saveReport(EvaluationReportDTO dto, Long userId, String ipAddress) {
        EvaluationReport report = mapper.toEntity(dto);
        LocalDateTime now = LocalDateTime.now();

        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        report.setCreatedBy(userId);
        report.setUpdatedBy(userId);
        report.setOriginIp(ipAddress);

        return mapper.toDto(repository.save(report));
    }

    @Transactional(readOnly = true)
    public EvaluationReport getRawReportBySimulation(java.util.UUID simulationId) {
        return repository.findBySimulationId(simulationId)
                .orElseThrow(() -> new RuntimeException("Evaluation report missing for target simulation context"));
    }
}