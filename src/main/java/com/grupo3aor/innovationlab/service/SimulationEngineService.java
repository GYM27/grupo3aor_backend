package com.grupo3aor.innovationlab.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.UUID;


import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupo3aor.innovationlab.dto.AlertDTO;
import com.grupo3aor.innovationlab.dto.MetricDTO;
import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import com.grupo3aor.innovationlab.repository.SimulationRepository;

@Slf4j
@RequiredArgsConstructor
@Service
public class SimulationEngineService {

    private final SimulationRepository simulationRepository;
    private final PhysiologicalReadingService physiologicalReadingService;
    private final ObjectMapper objectMapper;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final GlobalSettingsService globalSettingsService;
    private final DataPersistenceComponent dataPersistenceComponent;
    private final RuleEvaluatorService ruleEvaluatorService;

    // Cache to hold parsed JSON payloads, avoiding ObjectMapper overhead every second
    private final ConcurrentHashMap<UUID, List<MetricDTO>> metricsCache = new ConcurrentHashMap<>();
    
    // Cache to hold active simulations for degraded mode
    private final ConcurrentHashMap<UUID, Simulation> activeSimulationsCache = new ConcurrentHashMap<>();

    // Counter to avoid polling the database when no simulations are active
    private final AtomicInteger activeSimulationsCount = new AtomicInteger(0);

    // Exposing these methods so the SimulationService can notify the engine
    public void incrementActiveSimulations() {
        activeSimulationsCount.incrementAndGet();
    }

    public void decrementActiveSimulations() {
        if (activeSimulationsCount.get() > 0) {
            activeSimulationsCount.decrementAndGet();
        }
    }

    // Defining these constants to avoid magic strings
    private static final String HEART_RATE = "HeartRate";
    private static final String RESPIRATION_RATE = "RespirationRate";
    private static final String ARTERIAL_PRESSURE_SYSTOLIC = "ArterialPressure_Systolic";
    private static final String ARTERIAL_PRESSURE_DIASTOLIC = "ArterialPressure_Diastolic";

    /**
     * Canceling any simulations left in an active state from a previous server session.
     * Without this, the engine would keep generating readings for orphan simulations indefinitely after a restart.
     */
    @PostConstruct
    @Transactional
    public void cancelOrphanSimulations() {
        List<Simulation> orphans = simulationRepository.findAllByStatusIn(
            List.of(SimulationStatus.INICIADA, SimulationStatus.EM_CURSO)
        );

        if (!orphans.isEmpty()) {
            log.warn("[ENGINE] Found {} orphan simulation(s) from a previous session — cancelling them now.", orphans.size());
            for (Simulation sim : orphans) {
                sim.setStatus(SimulationStatus.CANCELADA);
                sim.setEndedAt(LocalDateTime.now());
                simulationRepository.save(sim);
                log.warn("[ENGINE] Simulation {} marked as CANCELADA.", sim.getId());
            }
        } else {
            log.info("[ENGINE] No orphan simulations found. Clean startup.");
        }
    }

    // Running this every second provides smooth playback of the JSON metrics
    @Scheduled(fixedRate = 1000)
    public void generateContinuousData() {
        // Short-circuiting here saves database hits if no simulation is active!
        if (activeSimulationsCount.get() == 0) {
            return;
        }

        List<Simulation> simulacoes;
        if (globalSettingsService.isDbFailed()) {
            simulacoes = new ArrayList<>(activeSimulationsCache.values());
        } else {
            simulacoes = simulationRepository.findAllByStatusIn(
                List.of(SimulationStatus.INICIADA, SimulationStatus.EM_CURSO)
            );
            activeSimulationsCache.clear();
            for (Simulation s : simulacoes) {
                activeSimulationsCache.put(s.getId(), s);
            }
        }

        for (Simulation s : simulacoes) {
            processSimulationPlayback(s);
        }
    }

    private void processSimulationPlayback(Simulation sim) {
        String payload = sim.getScenario().getMetricsPayload();
        if (payload == null || payload.isBlank()) {
            log.info("[ENGINE DEBUG] Ignoring BioGears simulation {}", sim.getId());
            // BioGears simulations do not have a metrics payload.
            // They are driven by the CSV batch upload and the frontend timeline.
            // We just ignore them in the engine loop and let the frontend explicitly stop them.
            return;
        }

        try {
            List<MetricDTO> metrics = metricsCache.computeIfAbsent(sim.getId(), id -> {
                try {
                    return objectMapper.readValue(payload, new TypeReference<List<MetricDTO>>() {});
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            if (metrics.isEmpty()) {
                // BioGears simulations have an empty metrics array [] in their dummy scenario.
                // We should NOT finalize them, because the frontend controls the timeline.
                return;
            }

            int currentIndex = sim.getNextMetricIndex();
            
            // Reached the end of the JSON array, so finish the simulation
            if (currentIndex >= metrics.size()) {
                log.info("[ENGINE] Simulation {} reached the end of the JSON file. Marking as FINALIZADA.", sim.getId());
                finalizeSimulation(sim);
                return;
            }

            // Calculating the elapsed time since the simulation started
            long elapsedMillis = Duration.between(sim.getStartedAt(), LocalDateTime.now()).toMillis();
            
            // Using the very first timestamp in the JSON to act as time 0 (T=0)
            Instant firstTimestamp = Instant.parse(metrics.get(0).getTimestamp());

            boolean hasAdvanced = false;
            List<PhysiologicalReadingDTO> batchToInsert = new ArrayList<>();

            // Processing all metrics that are due up to this elapsed time
            while (currentIndex < metrics.size()) {
                MetricDTO nextMetric = metrics.get(currentIndex);
                Instant metricInstant = Instant.parse(nextMetric.getTimestamp());
                
                long metricRelativeTimeMillis = Duration.between(firstTimestamp, metricInstant).toMillis();

                // Waiting for the next tick if this metric is still in the future relative to the playback
                if (metricRelativeTimeMillis > elapsedMillis) {
                    break;
                }

                // It's due! Creating the reading using the EXACT timestamp from the JSON, as requested
                PhysiologicalReadingDTO dto = PhysiologicalReadingDTO.builder()
                        .simulationId(sim.getId())
                        .handle(nextMetric.getHandle())
                        .unit(nextMetric.getUnit())
                        .value(nextMetric.getValue())
                        .timestamp(LocalDateTime.ofInstant(metricInstant, ZoneId.systemDefault()))
                        .build();

                batchToInsert.add(dto);
                
                currentIndex++;
                hasAdvanced = true;
            }
            
            if (!batchToInsert.isEmpty()) {
                if (globalSettingsService.isDbFailed()) {
                    List<PhysiologicalReading> entitiesForEvaluation = new ArrayList<>();
                    
                    for (PhysiologicalReadingDTO dto : batchToInsert) {
                        PhysiologicalReading reading = new PhysiologicalReading();
                        reading.setId(UUID.randomUUID());
                        reading.setSimulation(sim);
                        reading.setHandle(dto.getHandle());
                        reading.setUnit(dto.getUnit());
                        reading.setValue(dto.getValue());
                        reading.setTimestamp(dto.getTimestamp());
                        reading.setCreatedBy("engine@innovationlab.com");
                        reading.setUpdatedBy("engine@innovationlab.com");
                        reading.setOriginIp("127.0.0.1");

                        entitiesForEvaluation.add(reading);
                        dataPersistenceComponent.queueReading(reading);
                        
                        // Push to WebSocket directly
                        dto.setId(reading.getId());
                        messagingTemplate.convertAndSend("/topic/simulations/" + sim.getId() + "/readings", dto);
                    }
                    
                    // Evaluate rules in RAM during Degraded Mode
                    try {
                        ruleEvaluatorService.evaluateReadingsBatch(entitiesForEvaluation);
                    } catch (Exception e) {
                        log.error("[ENGINE] Failed to evaluate rules in RAM for Simulation {}", sim.getId(), e);
                    }
                } else {
                    for (PhysiologicalReadingDTO reading : batchToInsert) {
                        physiologicalReadingService.createReading(reading, "engine@innovationlab.com", "127.0.0.1");
                    }
                }
            }

            // Saving the new progress index since something was processed
            if (hasAdvanced) {
                sim.setNextMetricIndex(currentIndex);
                // Updating the status to EM_CURSO if it was INICIADA
                if (sim.getStatus() == SimulationStatus.INICIADA) {
                    sim.setStatus(SimulationStatus.EM_CURSO);
                }
                
                if (globalSettingsService.isDbFailed()) {
                    dataPersistenceComponent.queueSimulation(sim);
                } else {
                    simulationRepository.save(sim);
                }
            }

        } catch (Exception e) {
            log.error("[ENGINE] Failed to parse metrics for Simulation {}", sim.getId(), e);
            finalizeSimulation(sim);
        }
    }

    private void finalizeSimulation(Simulation sim) {
        sim.setStatus(SimulationStatus.FINALIZADA);
        sim.setEndedAt(LocalDateTime.now());
        
        if (globalSettingsService.isDbFailed()) {
            dataPersistenceComponent.queueSimulation(sim);
        } else {
            simulationRepository.save(sim);
        }
        
        decrementActiveSimulations();
        metricsCache.remove(sim.getId());
        
        try {
            AlertDTO finishAlert = new AlertDTO();
            finishAlert.setTimestamp(LocalDateTime.now());
            finishAlert.setSeverity("INFO");
            finishAlert.setSystemName("[SYSTEM_END_SIMULATION]");
            finishAlert.setValueAtTrigger(0.0);
            finishAlert.setSimulationId(sim.getId());
            messagingTemplate.convertAndSend("/topic/simulations/" + sim.getId() + "/alerts", finishAlert);
        } catch (Exception e) {
            log.warn("Failed to broadcast end of simulation for {}", sim.getId(), e);
        }
    }
}
