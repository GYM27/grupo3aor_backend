package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j // Adds the Logger (Lombok)
@RequiredArgsConstructor // Removes the manual constructor
@Service
/**
 * Service for managing physiological readings and telemetry batches.
 */
public class PhysiologicalReadingService {

    private final PhysiologicalReadingRepository repository;
    private final SimulationMapper mapper;
    private final SimulationRepository simulationRepository;
    private final RuleEvaluatorService ruleEvaluatorService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    @Transactional
    public PhysiologicalReadingDTO createReading(PhysiologicalReadingDTO dto, String userEmail, String ipAddress) {
        Simulation simulation = simulationRepository.findById(dto.getSimulationId())
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + dto.getSimulationId()));

        if (simulation.getStatus() == com.grupo3aor.innovationlab.domain.enums.SimulationStatus.FINALIZADA || 
            simulation.getStatus() == com.grupo3aor.innovationlab.domain.enums.SimulationStatus.CANCELADA) {
            throw new IllegalStateException("Cannot add readings to a finalized or canceled simulation.");
        }

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
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            if (dtos == null || dtos.isEmpty()) return new ArrayList<>();
            log.info("Processing telemetry batch of {} readings", dtos.size());
        
        meterRegistry.summary("vitalsim.readings.batch.size").record(dtos.size());

        // 1. Local cache to avoid hitting the DB for the same simulation multiple times in a single batch
        Map<UUID, Simulation> simulationCache = new HashMap<>();
        List<PhysiologicalReading> entitiesToSave = new ArrayList<>();

        // 2. Prepare all the entities
        for (PhysiologicalReadingDTO dto : dtos) {
            Simulation simulation = simulationCache.computeIfAbsent(
                dto.getSimulationId(), 
                this::getSimulationOrThrow
            );

            if (simulation.getStatus() == com.grupo3aor.innovationlab.domain.enums.SimulationStatus.FINALIZADA || 
                simulation.getStatus() == com.grupo3aor.innovationlab.domain.enums.SimulationStatus.CANCELADA) {
                throw new IllegalStateException("Cannot add readings to a finalized or canceled simulation.");
            }
            
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

        // 4. If the batch is small (Live Stream micro-batch), save and broadcast via WebSocket.
        // If it's huge (CSV 52k lines), skip to avoid blocking the Database and RAM.
        if (entitiesToSave.size() < 1000) {
            List<PhysiologicalReading> savedReadings = repository.saveAll(entitiesToSave);
            for (PhysiologicalReading savedReading : savedReadings) {
                PhysiologicalReadingDTO savedDto = mapper.toDto(savedReading);
                messagingTemplate.convertAndSend("/topic/simulations/" + savedDto.getSimulationId() + "/readings", savedDto);
            }
        }

        // We just return the DTOs (with null IDs for large batches) so the frontend is happy.
            return entitiesToSave.stream()
                    .map(mapper::toDto)
                    .toList();
        } finally {
            sample.stop(meterRegistry.timer("vitalsim.readings.batch.save.time", "description", "Time taken to process and save a CSV batch upload"));
        }
    }

    @Transactional(readOnly = true)
    public List<PhysiologicalReadingDTO> getReadingsBySimulation(UUID simulationId) {
        return repository.findBySimulation_Id(simulationId).stream()
                .map(mapper::toDto)
                .toList(); // Cleaner (Java 16+)
    }

    @Transactional(readOnly = true)
    public boolean isSimulationActive(UUID simulationId) {
        return simulationRepository.findById(simulationId)
                .map(sim -> sim.getStatus() == SimulationStatus.INICIADA || 
                            sim.getStatus() == SimulationStatus.EM_CURSO)
                .orElse(false);
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