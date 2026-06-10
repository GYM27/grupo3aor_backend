package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PhysiologicalReadingService {

    private final PhysiologicalReadingRepository repository;
    private final SimulationMapper mapper;
    private final SimulationRepository simulationRepository;

    public PhysiologicalReadingService(PhysiologicalReadingRepository repository, SimulationMapper mapper,
                                       SimulationRepository simulationRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.simulationRepository = simulationRepository;
    }

    @Transactional
    public PhysiologicalReadingDTO createReading(PhysiologicalReadingDTO dto, String userEmail, String ipAddress) {
        // I resolve the Simulation entity first to establish a proper FK relation.
        // This ensures readings can only be created for simulations that actually exist.
        Simulation simulation = simulationRepository.findById(dto.getSimulationId())
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + dto.getSimulationId()));

        PhysiologicalReading reading = mapper.toEntity(dto, simulation);

        reading.setCreatedBy(userEmail);
        reading.setUpdatedBy(userEmail);
        reading.setOriginIp(ipAddress);

        return mapper.toDto(repository.save(reading));
    }

    @Transactional(readOnly = true)
    public List<PhysiologicalReadingDTO> getReadingsBySimulation(UUID simulationId) {
        return repository.findBySimulation_Id(simulationId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}