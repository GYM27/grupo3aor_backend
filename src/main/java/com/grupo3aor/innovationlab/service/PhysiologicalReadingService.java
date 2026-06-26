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
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    private DegradedModeBufferService bufferService;

    @Autowired
    private DataPersistenceComponent persistenceComponent;

    public PhysiologicalReadingDTO createReading(PhysiologicalReadingDTO dto, String userEmail, String ipAddress) {
        Simulation simulation = simulationRepository.findById(dto.getSimulationId())
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found..."));

        PhysiologicalReading reading = mapper.toEntity(dto, simulation);
        
        // CRÍTICO: Gerar o ID atempadamente na memória para manter a integridade!
        if (reading.getId() == null) {
            reading.setId(UUID.randomUUID());
        }

        reading.setCreatedBy(userEmail);
        reading.setUpdatedBy(userEmail);
        reading.setOriginIp(ipAddress);

        PhysiologicalReadingDTO outDto = mapper.toDto(reading);

        // 1. Notificar os clientes PRIMEIRO (Tempo real não é interrompido)
        messagingTemplate.convertAndSend("/topic/simulations/" + dto.getSimulationId() + "/readings", outDto);

        // 2. Avaliar Regras (Isto também deverá ser adaptado para gerar os Alertas em memória com UUID)
        try {
            ruleEvaluatorService.evaluateReading(reading);
        } catch (Exception e){
            log.error("Erro na avaliação de regras: ", e);
        }

        // 3. O Circuit Breaker: Tentar gravar na Base de Dados
        if (bufferService.isDegraded()) {
            bufferService.addPendingReading(reading);
        } else {
            try {
                persistenceComponent.saveReadingSafely(reading);
            } catch (Exception e) { // Apanha DataAccessException, falhas de rede, etc.
                log.warn("[CIRCUIT BREAKER] Falha na Base de Dados! A entrar em Modo Degradado.");
                bufferService.setDegraded(true);
                bufferService.addPendingReading(reading);
            }
        }

        return outDto;
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

        Map<UUID, Simulation> simulationCache = new HashMap<>();
        List<PhysiologicalReading> entitiesToSave = new ArrayList<>();

        // 1. Preparar Entidades e GERAR OS UUIDs atempadamente
        for (PhysiologicalReadingDTO dto : dtos) {
            Simulation simulation = simulationCache.computeIfAbsent(
                dto.getSimulationId(), 
                this::getSimulationOrThrow // Idealmente usar também uma cache para as Simulações
            );
            
            PhysiologicalReading reading = mapper.toEntity(dto, simulation);
            if(reading.getId() == null) reading.setId(UUID.randomUUID()); // Gerar ID
            reading.setCreatedBy(userEmail);
            reading.setUpdatedBy(userEmail);
            reading.setOriginIp(ipAddress);
            entitiesToSave.add(reading);
        }

        // 2. Enviar para Websockets e Avaliar Regras (Totalmente independente da BD agora)
        for (PhysiologicalReading reading : entitiesToSave) {
            messagingTemplate.convertAndSend("/topic/simulations/" + reading.getSimulation().getId() + "/readings", mapper.toDto(reading));
        }
        
        // Avaliar todas as regras num formato otimizado para não bloquear a BD nem duplicar alertas (Isolamento de Transação)
        try {
            ruleEvaluatorService.evaluateReadingsBatch(entitiesToSave); 
        } catch (Exception e) {
            log.error("Erro ao avaliar batch de regras: ", e);
        }

        // 3. Circuit Breaker para o Batch
        if (bufferService.isDegraded()) {
            entitiesToSave.forEach(bufferService::addPendingReading);
        } else {
            try {
                // Necessário criar no DataPersistenceComponent
                persistenceComponent.saveAllReadingsSafely(entitiesToSave);
            } catch (Exception e) {
                log.warn("[CIRCUIT BREAKER BATCH] Falha na Base de Dados! Batch de {} enviado para o buffer.", entitiesToSave.size());
                bufferService.setDegraded(true);
                entitiesToSave.forEach(bufferService::addPendingReading);
            }
        }

        return entitiesToSave.stream().map(mapper::toDto).toList();
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