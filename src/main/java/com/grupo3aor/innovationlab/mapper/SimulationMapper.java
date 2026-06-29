package com.grupo3aor.innovationlab.mapper;

import com.grupo3aor.innovationlab.domain.entity.*;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import com.grupo3aor.innovationlab.dto.*;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Component responsible for clean object-to-object translation between
 * JPA database entities and data transfer contracts.
 * <p>
 * I updated this mapper to work with the new @ManyToOne FK relations in Alert,
 * PhysiologicalReading and EvaluationReport. The toEntity methods now receive the
 * resolved Simulation (and Rule) JPA entity so Hibernate can properly set the FK.
 * The toDto methods extract the UUID from the relation for a stable API contract.
 * </p>
 */
@Component
public class SimulationMapper {

    // I split toEntity into two variants: one for reads (no simulation needed)
    // and one for writes (simulation entity required to set the FK).

    public PhysiologicalReading toEntity(PhysiologicalReadingDTO dto, Simulation simulation) {
        PhysiologicalReading entity = new PhysiologicalReading();
        entity.setSimulation(simulation);
        entity.setHandle(dto.getHandle());
        entity.setUnit(dto.getUnit());
        entity.setValue(dto.getValue());
        entity.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now());
        return entity;
    }

    public PhysiologicalReadingDTO toDto(PhysiologicalReading entity) {
        PhysiologicalReadingDTO dto = new PhysiologicalReadingDTO();
        dto.setId(entity.getId());
        // I extract the UUID from the FK relation so the API response stays consistent
        dto.setSimulationId(entity.getSimulation() != null ? entity.getSimulation().getId() : null);
        dto.setHandle(entity.getHandle());
        dto.setUnit(entity.getUnit());
        dto.setValue(entity.getValue());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }

    public Alert toEntity(AlertDTO dto, Simulation simulation, Rule rule) {
        Alert entity = new Alert();
        entity.setSimulation(simulation);
        entity.setRule(rule);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : AlertStatus.ATIVO);
        entity.setValueAtTrigger(dto.getValueAtTrigger() != null ? dto.getValueAtTrigger().doubleValue() : null);
        return entity;
    }

    public AlertDTO toDto(Alert entity) {
        AlertDTO dto = new AlertDTO();
        dto.setId(entity.getId());
        // I extract the UUIDs from the FK relations so the API response stays consistent
        dto.setSimulationId(entity.getSimulation() != null ? entity.getSimulation().getId() : null);
        dto.setRuleId(entity.getRule() != null ? entity.getRule().getId() : null);
        dto.setTimestamp(entity.getTimestamp());
        dto.setStatus(entity.getStatus());
        dto.setValueAtTrigger(entity.getValueAtTrigger());
        if (entity.getRule() != null) {
            dto.setSeverity(entity.getRule().getSeverity().name());
            dto.setRuleName(entity.getRule().getName());
            dto.setAnalyticalJustification(entity.getRule().getAnalyticalJustification());
            dto.setFormattedValue(com.grupo3aor.innovationlab.util.ClinicalFormatter.formatClinicalMessage(entity));
            if (entity.getRule().getSystem() != null) {
                dto.setSystemName(entity.getRule().getSystem().getSystemName());
            }
        }
        return dto;
    }

    public EvaluationReport toEntity(EvaluationReportDTO dto, Simulation simulation) {
        EvaluationReport entity = new EvaluationReport();
        entity.setSimulation(simulation);
        entity.setIntervaloTemporal(dto.getIntervaloTemporal());
        entity.setRationaleText(dto.getRationaleText());
        entity.setPdfContent(dto.getPdfContent() != null ? dto.getPdfContent() : new byte[0]);
        entity.setStartObservation(dto.getStartObservation());
        entity.setEndObservation(dto.getEndObservation());
        return entity;
    }

    public EvaluationReportDTO toDto(EvaluationReport entity) {
        EvaluationReportDTO dto = new EvaluationReportDTO();
        dto.setId(entity.getId());
        // I extract the UUID from the FK relation so the API response stays consistent
        dto.setSimulationId(entity.getSimulation() != null ? entity.getSimulation().getId() : null);
        dto.setIntervaloTemporal(entity.getIntervaloTemporal());
        dto.setRationaleText(entity.getRationaleText());
        dto.setPdfContent(entity.getPdfContent());
        dto.setStartObservation(entity.getStartObservation());
        dto.setEndObservation(entity.getEndObservation());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
