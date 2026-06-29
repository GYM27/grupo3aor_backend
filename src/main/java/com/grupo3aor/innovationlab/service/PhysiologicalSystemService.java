package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalSystem;
import com.grupo3aor.innovationlab.dto.PhysiologicalSystemRequest;
import com.grupo3aor.innovationlab.dto.PhysiologicalSystemResponse;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.repository.PhysiologicalSystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic tier managing Physiological Systems.
 * <p>
 * I implemented this service to isolate transaction boundaries and business validation 
 * rules, preventing the REST controllers from directly manipulating persistent data.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhysiologicalSystemService {

    private final PhysiologicalSystemRepository repository;

    /**
     * Instantiates and persists a new physiological system configuration.
     * @param request Inbound structural payload.
     * @param operatorEmail Identifier of the executing administrator.
     * @param originIp Physical network origin of the request.
     * @return Outbound safe summary of the created resource.
     */
    @Transactional
    public PhysiologicalSystemResponse createSystem(PhysiologicalSystemRequest request, String operatorEmail, String originIp) {
        // I enforced a strict uniqueness check here before touching the persistence context
        // to return a clear operational error instead of letting the SQL constraints crash.
        if (repository.findBySystemName(request.getSystemName()).isPresent()) {
            log.warn("[BUSINESS_VALIDATION] Action: CREATE_SYSTEM_FAILED | Reason: NAME_ALREADY_EXISTS | Target: {} | Operator: {}", 
                     request.getSystemName(), operatorEmail);
            throw new IllegalArgumentException("A physiological system with this name already exists.");
        }

        // I manually mapped the DTO payload into the physical entity model, injecting 
        // the auditing metadata captured from the controller layer.
        PhysiologicalSystem system = PhysiologicalSystem.builder()
                .systemName(request.getSystemName())
                .createdBy(operatorEmail)
                .updatedBy(operatorEmail)
                .originIp(originIp)
                .build();

        PhysiologicalSystem savedSystem = repository.save(system);
        
        log.info("[AUDIT] Action: SYSTEM_CREATED | Target ID: {} | Target Name: {} | Operator: {} | IP: {}", 
                 savedSystem.getId(), savedSystem.getSystemName(), operatorEmail, originIp);

        return mapToResponse(savedSystem);
    }

    /**
     * Retrieves all functional, non-deleted physiological systems.
     * @return Collection of safe system summaries.
     */
    @Transactional(readOnly = true)
    public List<PhysiologicalSystemResponse> getAllActiveSystems() {
        // I delegated the active filtering responsibility to the repository query 
        // to prevent large datasets from being loaded into memory unnecessarily.
        return repository.findAllByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Safely executes a logical removal of a target system.
     * @param id Database identifier of the system.
     * @param operatorEmail Administrator issuing the deletion command.
     */
    @Transactional
    public void deleteSystem(Long id) {
        PhysiologicalSystem system = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System not found with ID: " + id));

        // The @SQLDelete annotation on the entity model intercepts this instruction
        // and translates it into an UPDATE statement, securing historical data.
        repository.delete(system);
    }

    /**
     * Retrieves a single system by its ID.
     */
    @Transactional(readOnly = true)
    public PhysiologicalSystemResponse getSystemById(Long id) {
        PhysiologicalSystem system = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System not found with ID: " + id));
        return mapToResponse(system);
    }

    /**
     * Updates an existing physiological system.
     */
    @Transactional
    public PhysiologicalSystemResponse updateSystem(Long id, PhysiologicalSystemRequest request) {
        PhysiologicalSystem system = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System not found with ID: " + id));

        if (!system.getSystemName().equalsIgnoreCase(request.getSystemName()) &&
            repository.findBySystemName(request.getSystemName()).isPresent()) {
            throw new IllegalArgumentException("A physiological system with this name already exists.");
        }

        system.setSystemName(request.getSystemName());

        PhysiologicalSystem updatedSystem = repository.save(system);

        return mapToResponse(updatedSystem);
    }

    /**
     * Helper conversion mechanism.
     */
    private PhysiologicalSystemResponse mapToResponse(PhysiologicalSystem entity) {
        return PhysiologicalSystemResponse.builder()
                .id(entity.getId())
                .systemName(entity.getSystemName())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
