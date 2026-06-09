package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.entity.ClinicalScenario;
import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import com.grupo3aor.innovationlab.dto.SimulationRequest;
import com.grupo3aor.innovationlab.dto.SimulationResponse;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import com.grupo3aor.innovationlab.repository.UserRepository;
import com.grupo3aor.innovationlab.repository.ClinicalScenarioRepository;
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
 * Service layer for managing simulations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final UserRepository userRepository;
    private final ClinicalScenarioRepository scenarioRepository;

    /**
     * Starts a new simulation securely from the user's request.
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
                .status(SimulationStatus.INICIADA)
                .startedAt(LocalDateTime.now())
                .build();

        Simulation saved = simulationRepository.save(sim);
        return mapToResponse(saved);
    }

    /**
     * Ends an ongoing simulation.
     */
    @Transactional
    public SimulationResponse stopSimulation(UUID simulationId) {
        Simulation sim = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation not found with ID: " + simulationId));

        // Let's protect it so we don't end a simulation that is already finished or canceled!
        if (sim.getStatus() == SimulationStatus.FINALIZADA || sim.getStatus() == SimulationStatus.CANCELADA) {
            throw new IllegalStateException("Simulation is already finalized or canceled.");
        }

        sim.setStatus(SimulationStatus.FINALIZADA);
        sim.setEndedAt(LocalDateTime.now());

        Simulation saved = simulationRepository.save(sim);
        return mapToResponse(saved);
    }

    /**
     * Gets all historical simulations.
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
        return mapToResponse(saved);
    }

    // =========================================================
    // HELPER MAPPERS
    // =========================================================
    private SimulationResponse mapToResponse(Simulation sim) {
        return SimulationResponse.builder()
                .id(sim.getId())
                .scenarioId(sim.getScenario() != null ? sim.getScenario().getId() : null)
                .userEmail(sim.getUser() != null ? sim.getUser().getEmail() : "Unknown")
                .startedAt(sim.getStartedAt())
                .endedAt(sim.getEndedAt())
                .status(sim.getStatus())
                .build();
    }
}
