package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j // Adds the Logger (Lombok)
@RequiredArgsConstructor // Removes the manual constructor
@Service
public class PhysiologicalReadingService {

    private final PhysiologicalReadingRepository repository;
    private final SimulationMapper mapper;
    private final SimulationRepository simulationRepository;
    private final RuleEvaluatorService ruleEvaluatorService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public PhysiologicalReadingDTO createReading(PhysiologicalReadingDTO dto, String userEmail, String ipAddress) {
        Simulation simulation = simulationRepository.findById(dto.getSimulationId())
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + dto.getSimulationId()));

        PhysiologicalReading reading = mapper.toEntity(dto, simulation);

        reading.setCreatedBy(userEmail);
        reading.setUpdatedBy(userEmail);
        reading.setOriginIp(ipAddress);
        
        // Save to DB
        PhysiologicalReading savedReading = repository.save(reading);
        PhysiologicalReadingDTO savedDto = mapper.toDto(savedReading);
        
        // Push the new reading to the WebSocket topic for this specific simulation
        messagingTemplate.convertAndSend("/topic/simulations/" + dto.getSimulationId() + "/readings", savedDto);

        try {
            ruleEvaluatorService.evaluateReading(savedReading);
        } catch (Exception e){
            e.printStackTrace();
        }

        return savedDto;
    }

    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void createReadingAsync(PhysiologicalReadingDTO dto, String userEmail, String ipAddress) {
        try {
            createReading(dto, userEmail, ipAddress);
        } catch (Exception e) {
            log.error("Failed to process async telemetry reading: {}", e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void createReadingBatchAsync(List<PhysiologicalReadingDTO> dtos, String userEmail, String ipAddress) {
        try {
            createReadingBatch(dtos, userEmail, ipAddress);
        } catch (Exception e) {
            log.error("Failed to process async telemetry batch: {}", e.getMessage());
        }
    }

    @Transactional
    public List<PhysiologicalReadingDTO> createReadingBatch(List<PhysiologicalReadingDTO> dtos, String userEmail, String ipAddress) {
        if (dtos == null || dtos.isEmpty()) return new ArrayList<>();
        log.info("Processing telemetry batch of {} readings", dtos.size());

        // 1. Local cache to avoid hitting the DB for the same simulation multiple times in a single batch
        Map<UUID, Simulation> simulationCache = new HashMap<>();
        List<PhysiologicalReading> entitiesToSave = new ArrayList<>();

        // 2. Prepare all the entities
        for (PhysiologicalReadingDTO dto : dtos) {
            Simulation simulation = simulationCache.computeIfAbsent(
                dto.getSimulationId(), 
                this::getSimulationOrThrow
            );
            
            PhysiologicalReading reading = mapper.toEntity(dto, simulation);
            reading.setCreatedBy(userEmail);
            reading.setUpdatedBy(userEmail);
            reading.setOriginIp(ipAddress);
            
            entitiesToSave.add(reading);
        }

        // 3. Evaluate rules locally in an optimized batch BEFORE hitting the database.
        // This pure RAM computation is extremely fast and ensures alerts are broadcasted
        // instantly via WebSocket without waiting for the slow DB insert.
        try {
            ruleEvaluatorService.evaluateReadingsBatch(entitiesToSave);
        } catch (Exception e) {
            log.error("Failed to evaluate rules for batch: {}", e.getMessage(), e);
        }

        // 4. Do NOT save the 52,000 readings into the database!
        // We only needed them in RAM to evaluate the rules and generate the Alerts.
        // Saving 52,000 rows takes 1.5 minutes and causes massive DB locking ("pending" requests).
        // Since the frontend replays BioGears locally, it never fetches these readings.
        // We just return the DTOs (with null IDs) so the frontend is happy.
        return entitiesToSave.stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PhysiologicalReadingDTO> getReadingsBySimulation(UUID simulationId) {
        return repository.findBySimulation_Id(simulationId).stream()
                .map(mapper::toDto)
                .toList(); // Cleaner (Java 16+)
    }

    // --- Helper Methods to keep the code DRY (Don't Repeat Yourself) ---

    private Simulation getSimulationOrThrow(UUID simulationId) {
        return simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + simulationId));
    }

    private PhysiologicalReading processSingleReading(PhysiologicalReadingDTO dto, Simulation simulation, String userEmail, String ipAddress) {
        PhysiologicalReading reading = mapper.toEntity(dto, simulation);
        reading.setCreatedBy(userEmail);
        reading.setUpdatedBy(userEmail);
        reading.setOriginIp(ipAddress);
        return repository.save(reading);
    }

    private PhysiologicalReadingDTO publishAndEvaluate(PhysiologicalReading savedReading) {
        PhysiologicalReadingDTO savedDto = mapper.toDto(savedReading);

        // Send to a simulation-specific topic, not a global one!
        String destination = "/topic/simulations/" + savedReading.getSimulation().getId() + "/readings";
        messagingTemplate.convertAndSend(destination, savedDto);

        try {
            ruleEvaluatorService.evaluateReading(savedReading);
        } catch (Exception e) {
            // Proper error logging for production
            log.error("Failed to evaluate rules for reading ID {}: {}", savedReading.getId(), e.getMessage(), e);
        }

        return savedDto;
    }
}