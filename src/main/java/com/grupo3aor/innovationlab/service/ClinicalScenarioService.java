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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic tier managing Clinical Scenarios.
 * <p>
 * I implemented this abstraction to securely handle medical scenario management,
 * executing business checks prior to communicating with the relational persistence layer.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalScenarioService {

    private final ClinicalScenarioRepository repository;

    /**
     * Instantiates and persists a new clinical scenario configuration.
     * * @param request Inbound structural payload.
     * @param operatorEmail Identifier of the executing administrator.
     * @param originIp Physical network origin of the request.
     * @return Outbound safe summary of the created resource.
     */
    @Transactional
    public ClinicalScenarioResponse createScenario(ClinicalScenarioRequest request, String operatorEmail, String originIp) {
        if (repository.findByName(request.getName()).isPresent()) {
            log.warn("[BUSINESS_VALIDATION] Action: CREATE_SCENARIO_FAILED | Reason: NAME_ALREADY_EXISTS | Target: {} | Operator: {}", 
                     request.getName(), operatorEmail);
            throw new IllegalArgumentException("A clinical scenario with this name already exists.");
        }

        // I transferred the verified properties into a new physical representation,
        // attaching the immutable auditing tokens passed down from the REST boundary.
        ClinicalScenario scenario = ClinicalScenario.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(operatorEmail)
                .updatedBy(operatorEmail)
                .originIp(originIp)
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
                .collect(Collectors.toList());
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

        repository.delete(scenario);

        log.info("[AUDIT] Action: SCENARIO_SOFT_DELETED | Target ID: {} | Operator: {}", id, operatorEmail);
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
