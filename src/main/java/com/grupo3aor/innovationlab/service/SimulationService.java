package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.entity.ClinicalScenario;
import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import com.grupo3aor.innovationlab.dto.SimulationRequest;
import com.grupo3aor.innovationlab.dto.SimulationResponse;
import com.grupo3aor.innovationlab.dto.AlertEventDTO;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import com.grupo3aor.innovationlab.repository.UserRepository;
import com.grupo3aor.innovationlab.repository.ClinicalScenarioRepository;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Here is where we manage the core simulation lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final UserRepository userRepository;
    private final ClinicalScenarioRepository scenarioRepository;
    private final SimulationEngineService simulationEngineService;
    private final RuleEvaluatorService ruleEvaluatorService;
    private final PhysiologicalReadingRepository readingRepository;
    private final AlertRepository alertRepository;

    /**
     * Let's start a new simulation securely from the user's request.
     */
    @Transactional
    public SimulationResponse startSimulation(SimulationRequest request, String userEmail) {
        User starter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found in database!"));

        ClinicalScenario scenario = scenarioRepository.findById(request.getScenarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinical Scenario not found with ID: " + request.getScenarioId()));

        Simulation sim = Simulation.builder()
                .scenario(scenario)
                .user(starter)
                .status(SimulationStatus.EM_CURSO)
                .startedAt(LocalDateTime.now())
                .build();

        Simulation saved = simulationRepository.save(sim);
        
        // Time to wake up the engine so it starts polling!
        simulationEngineService.incrementActiveSimulations();
        
        return mapToResponse(saved);
    }

    /**
     * Safely ends an ongoing simulation.
     */
    @Transactional
    public SimulationResponse stopSimulation(UUID simulationId, Double cutOffSeconds) {
        Simulation sim = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + simulationId));

        // We must protect this block to prevent ending a simulation that is already finalized or canceled!
        if (sim.getStatus() == SimulationStatus.FINALIZADA || sim.getStatus() == SimulationStatus.CANCELADA) {
            throw new IllegalStateException("Simulation is already finalized or canceled.");
        }

        if (cutOffSeconds != null) {
            PhysiologicalReading firstReading = readingRepository.findFirstBySimulation_IdOrderByTimestampAsc(simulationId);
            if (firstReading != null) {
                LocalDateTime exactBaseTime = firstReading.getTimestamp();
                LocalDateTime cutOffAbsolute = exactBaseTime.plusNanos((long)(cutOffSeconds * 1_000_000_000L)).plusSeconds(1);
                readingRepository.bulkDeleteFutureReadings(simulationId, cutOffAbsolute);
                alertRepository.bulkDeleteFutureAlerts(simulationId, cutOffAbsolute);
                log.info("Truncated simulation {} data after {}", simulationId, cutOffAbsolute);
            }
        }

        sim.setStatus(SimulationStatus.FINALIZADA);
        sim.setEndedAt(LocalDateTime.now());

        Simulation saved = simulationRepository.save(sim);
        
        // Let the engine know that one less simulation is active
        simulationEngineService.decrementActiveSimulations();
        ruleEvaluatorService.clearSimulationState(simulationId);
        
        return mapToResponse(saved);
    }

    /**
     * Grabs the entire simulation execution history.
     */
    @Transactional(readOnly = true)
    public List<SimulationResponse> getHistory() {
        return simulationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancels an ongoing simulation.
     */
    @Transactional
    public SimulationResponse cancelSimulation(UUID simulationId) {
        Simulation sim = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + simulationId));

        if (sim.getStatus() == SimulationStatus.FINALIZADA || sim.getStatus() == SimulationStatus.CANCELADA) {
            throw new IllegalStateException("Simulation is already finalized or canceled.");
        }

        sim.setStatus(SimulationStatus.CANCELADA);
        sim.setEndedAt(LocalDateTime.now());

        Simulation saved = simulationRepository.save(sim);
        
        // Letting the engine know we just canceled one
        simulationEngineService.decrementActiveSimulations();
        ruleEvaluatorService.clearSimulationState(simulationId);
        
        return mapToResponse(saved);
    }

    /**
     * Pauses an ongoing simulation.
     */
    @Transactional
    public SimulationResponse pauseSimulation(UUID simulationId) {
        Simulation sim = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + simulationId));

        if (sim.getStatus() != SimulationStatus.EM_CURSO && sim.getStatus() != SimulationStatus.INICIADA) {
            throw new IllegalStateException("Only active simulations can be paused.");
        }

        sim.setStatus(SimulationStatus.PAUSADA);
        Simulation saved = simulationRepository.save(sim);

        // We can tell the engine to stop polling for this specific simulation
        simulationEngineService.decrementActiveSimulations();

        return mapToResponse(saved);
    }

    /**
     * Resumes a previously paused simulation.
     */
    @Transactional
    public SimulationResponse resumeSimulation(UUID simulationId) {
        Simulation sim = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + simulationId));

        if (sim.getStatus() != SimulationStatus.PAUSADA) {
            throw new IllegalStateException("Only paused simulations can be resumed.");
        }

        sim.setStatus(SimulationStatus.EM_CURSO);
        Simulation saved = simulationRepository.save(sim);

        // Simulation is back on track, wake the engine up again
        simulationEngineService.incrementActiveSimulations();

        return mapToResponse(saved);
    }

    // =========================================================
    // HELPER MAPPERS
    // =========================================================
    private SimulationResponse mapToResponse(Simulation sim) {
        String scenarioName = sim.getScenario() != null ? sim.getScenario().getName() : "Unknown Scenario";
        String studentName = sim.getUser() != null ? (sim.getUser().getFirstName() + " " + sim.getUser().getLastName()) : "Unknown User";
        
        List<AlertEventDTO> events = alertRepository.findBySimulation_Id(sim.getId()).stream()
                .map(alert -> {
                    String ruleName = (alert.getRule() != null && alert.getRule().getName() != null && !alert.getRule().getName().isEmpty())
                            ? alert.getRule().getName()
                            : "Sem Nome";
                    return AlertEventDTO.builder()
                        .timestamp(alert.getTimestamp())
                        .description("Regra " + ruleName + " acionada (" + String.format("%.1f", alert.getValueAtTrigger()) + ")")
                        .type(alert.getStatus().name().toLowerCase())
                        .build();
                })
                .collect(Collectors.toList());

        return SimulationResponse.builder()
                .id(sim.getId())
                .scenarioId(sim.getScenario() != null ? sim.getScenario().getId() : null)
                .scenarioName(scenarioName)
                .userEmail(sim.getUser() != null ? sim.getUser().getEmail() : "Unknown")
                .studentName(studentName)
                .startedAt(sim.getStartedAt())
                .endedAt(sim.getEndedAt())
                .status(sim.getStatus())
                .events(events)
                .build();
    }
}
