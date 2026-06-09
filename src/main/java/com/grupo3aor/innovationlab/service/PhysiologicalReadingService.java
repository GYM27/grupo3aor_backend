package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PhysiologicalReadingService {

    private final PhysiologicalReadingRepository repository;
    private final SimulationMapper mapper;

    public PhysiologicalReadingService(PhysiologicalReadingRepository repository, SimulationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public PhysiologicalReadingDTO createReading(PhysiologicalReadingDTO dto, Long userId, String ipAddress) {
        PhysiologicalReading reading = mapper.toEntity(dto);
        LocalDateTime now = LocalDateTime.now();

        reading.setCreatedAt(now);
        reading.setUpdatedAt(now);
        reading.setCreatedBy(userId);
        reading.setUpdatedBy(userId);
        reading.setOriginIp(ipAddress);

        return mapper.toDto(repository.save(reading));
    }

    @Transactional(readOnly = true)
    public List<PhysiologicalReadingDTO> getReadingsBySimulation(java.util.UUID simulationId) {
        return repository.findBySimulationId(simulationId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}