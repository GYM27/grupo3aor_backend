package com.grupo3aor.innovationlab.mapper;

import com.grupo3aor.innovationlab.domain.entity.*;
import com.grupo3aor.innovationlab.dto.*;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Component responsible for clean object-to-object translation between
 * JPA database entities and data transfer contracts.
 */
@Component
public class SimulationMapper {

    public PhysiologicalReading toEntity(PhysiologicalReadingDTO dto) {
        PhysiologicalReading entity = new PhysiologicalReading();
        entity.setSimulationId(dto.getSimulationId());
        entity.setHandle(dto.getHandle());
        entity.setUnit(dto.getUnit());
        entity.setValue(dto.getValue());
        entity.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now());
        return entity;
    }

    public PhysiologicalReadingDTO toDto(PhysiologicalReading entity) {
        PhysiologicalReadingDTO dto = new PhysiologicalReadingDTO();
        dto.setId(entity.getId());
        dto.setSimulationId(entity.getSimulationId());
        dto.setHandle(entity.getHandle());
        dto.setUnit(entity.getUnit());
        dto.setValue(entity.getValue());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }

    public Alert toEntity(AlertDTO dto) {
        Alert entity = new Alert();
        entity.setSimulationId(dto.getSimulationId());
        entity.setRuleId(dto.getRuleId());
        entity.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : AlertStatus.ATIVO);
        entity.setValueAtTrigger(dto.getValueAtTrigger());
        return entity;
    }

    public AlertDTO toDto(Alert entity) {
        AlertDTO dto = new AlertDTO();
        dto.setId(entity.getId());
        dto.setSimulationId(entity.getSimulationId());
        dto.setRuleId(entity.getRuleId());
        dto.setTimestamp(entity.getTimestamp());
        dto.setStatus(entity.getStatus());
        dto.setValueAtTrigger(entity.getValueAtTrigger());
        return dto;
    }

    public EvaluationReport toEntity(EvaluationReportDTO dto) {
        EvaluationReport entity = new EvaluationReport();
        entity.setSimulationId(dto.getSimulationId());
        entity.setIntervaloTemporal(dto.getIntervaloTemporal());
        entity.setRationaleText(dto.getRationaleText());
        entity.setPdfContent(dto.getPdfContent() != null ? dto.getPdfContent() : new byte[0]);
        return entity;
    }

    public EvaluationReportDTO toDto(EvaluationReport entity) {
        EvaluationReportDTO dto = new EvaluationReportDTO();
        dto.setId(entity.getId());
        dto.setSimulationId(entity.getSimulationId());
        dto.setIntervaloTemporal(entity.getIntervaloTemporal());
        dto.setRationaleText(entity.getRationaleText());
        dto.setPdfContent(entity.getPdfContent());
        return dto;
    }
}