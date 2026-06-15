<<<<<<< HEAD
package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final RuleEvaluatorService ruleEvaluatorService;
    private final SimpMessagingTemplate messagingTemplate;

    public PhysiologicalReadingService(PhysiologicalReadingRepository repository, SimulationMapper mapper,
                                       SimulationRepository simulationRepository,
                                       RuleEvaluatorService ruleEvaluatorService,
                                       SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.mapper = mapper;
        this.simulationRepository = simulationRepository;
        this.ruleEvaluatorService = ruleEvaluatorService;
        this.messagingTemplate = messagingTemplate;
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
        // Save to DB
        PhysiologicalReading savedReading = repository.save(reading);
        // Convert to DTO
        PhysiologicalReadingDTO savedDto = mapper.toDto(savedReading);
        // Send to WebSocket
        messagingTemplate.convertAndSend("/topic/readings", savedDto);

        try {
            ruleEvaluatorService.evaluateReading(savedReading);
        }catch (Exception e){
            e.printStackTrace();
        }

        return savedDto;
    }

    @Transactional(readOnly = true)
    public List<PhysiologicalReadingDTO> getReadingsBySimulation(UUID simulationId) {
        return repository.findBySimulation_Id(simulationId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
=======
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
import java.util.ArrayList;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
public class PhysiologicalReadingService {

    private final PhysiologicalReadingRepository repository;
    private final SimulationMapper mapper;
    private final SimulationRepository simulationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PhysiologicalReadingService(PhysiologicalReadingRepository repository, SimulationMapper mapper,
                                       SimulationRepository simulationRepository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.mapper = mapper;
        this.simulationRepository = simulationRepository;
        this.messagingTemplate = messagingTemplate;
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

        PhysiologicalReadingDTO savedDto = mapper.toDto(repository.save(reading));
        
        // Push the new reading to the WebSocket topic for this specific simulation
        messagingTemplate.convertAndSend("/topic/simulations/" + dto.getSimulationId() + "/readings", savedDto);
        
        return savedDto;
    }

    @Transactional
    public List<PhysiologicalReadingDTO> createReadingBatch(List<PhysiologicalReadingDTO> dtos, String userEmail, String ipAddress) {
        List<PhysiologicalReadingDTO> savedDtos = new ArrayList<>();
        
        for (PhysiologicalReadingDTO dto : dtos) {
            // Re-using the single create logic to ensure all relations and WebSocket pushes are triggered
            savedDtos.add(createReading(dto, userEmail, ipAddress));
        }
        
        return savedDtos;
    }

    @Transactional(readOnly = true)
    public List<PhysiologicalReadingDTO> getReadingsBySimulation(UUID simulationId) {
        return repository.findBySimulation_Id(simulationId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
>>>>>>> d9d57c2f9a3f3c82f444b2b421436c61df437317
