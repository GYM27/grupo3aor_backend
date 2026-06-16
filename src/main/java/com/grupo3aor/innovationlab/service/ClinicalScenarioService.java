package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.ClinicalScenario;
import com.grupo3aor.innovationlab.dto.ClinicalScenarioRequest;
import com.grupo3aor.innovationlab.dto.ClinicalScenarioResponse;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.repository.ClinicalScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Here we handle the business logic for Clinical Scenarios.
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalScenarioService {

    private final ClinicalScenarioRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Instantiates and persists a new clinical scenario configuration.
     * * @param request Inbound structural payload.
     * @param operatorEmail Identifier of the executing administrator.
     * @param originIp Physical network origin of the request.
     * @return Outbound safe summary of the created resource.
     */
    @Transactional
    public ClinicalScenarioResponse createScenario(ClinicalScenarioRequest request, String operatorEmail, String originIp) {
        String metricsJson = null;
        try {
            if (request.getMetrics() != null) {
                metricsJson = objectMapper.writeValueAsString(request.getMetrics());
            }
        } catch (Exception e) {
            log.error("Failed to serialize metrics for scenario", e);
        }

        ClinicalScenario scenario = ClinicalScenario.builder()
                .name(request.getName())
                .description(request.getDescription())
                .metricsPayload(metricsJson)
                .createdBy(operatorEmail)
                .updatedBy(operatorEmail)
                .originIp(originIp)
                .active(true) // Making sure it starts active (if applicable to the builder)
                .build();

        ClinicalScenario savedScenario = repository.save(scenario);

        log.info("[AUDIT] Action: SCENARIO_CREATED | Target ID: {} | Target Name: {} | Operator: {} | IP: {}", 
                 savedScenario.getId(), savedScenario.getName(), operatorEmail, originIp);

        return mapToResponse(savedScenario);
    }

    /**
     * Retrieves all functional, non-deleted clinical scenarios.
     * * @return Collection of safe scenario summaries.
     */
    @Transactional(readOnly = true)
    public List<ClinicalScenarioResponse> getAllActiveScenarios() {
        return repository.findAllByActiveTrue().stream()
                .map(this::mapToResponse)
                .toList(); // Using .toList() for Java 16+ (cleaner than Collectors.toList())
    }

    /**
     * Safely executes a logical removal of a target scenario.
     * * @param id Database identifier of the scenario.
     * @param operatorEmail Administrator issuing the deletion command.
     */
    @Transactional
    public void deleteScenario(Long id, String operatorEmail) {
        ClinicalScenario scenario = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with ID: " + id));

        // Fix: Executing a true Soft Delete
        scenario.setActive(false); 
        scenario.setUpdatedBy(operatorEmail);

        log.info("[AUDIT] Action: SCENARIO_SOFT_DELETED | Target ID: {} | Operator: {}", id, operatorEmail);
    }

    /**
     * Retrieves a single scenario by its ID.
     */
    @Transactional(readOnly = true)
    public ClinicalScenarioResponse getScenarioById(Long id) {
        ClinicalScenario scenario = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with ID: " + id));
        return mapToResponse(scenario);
    }

    /**
     * Updates an existing clinical scenario.
     */
    @Transactional
    public ClinicalScenarioResponse updateScenario(Long id, ClinicalScenarioRequest request, String operatorEmail, String originIp) {
        ClinicalScenario scenario = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with ID: " + id));

        scenario.setName(request.getName());
        scenario.setDescription(request.getDescription());
        scenario.setUpdatedBy(operatorEmail);

        try {
            if (request.getMetrics() != null) {
                scenario.setMetricsPayload(objectMapper.writeValueAsString(request.getMetrics()));
            }
        } catch (Exception e) {
            log.error("Failed to serialize metrics for scenario update", e);
        }

        // O repository.save(scenario) foi removido. O JPA Dirty Checking fará o update automático.

        log.info("[AUDIT] Action: SCENARIO_UPDATED | Target ID: {} | Operator: {} | IP: {}", 
                 scenario.getId(), operatorEmail, originIp);

        return mapToResponse(scenario);
    }

    /**
     * Helper conversion mechanism.
     */
    private ClinicalScenarioResponse mapToResponse(ClinicalScenario entity) {
        return ClinicalScenarioResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}