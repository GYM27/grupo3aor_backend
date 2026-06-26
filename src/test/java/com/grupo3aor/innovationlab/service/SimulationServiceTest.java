package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.ClinicalScenario;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import com.grupo3aor.innovationlab.dto.SimulationRequest;
import com.grupo3aor.innovationlab.dto.SimulationResponse;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.repository.ClinicalScenarioRepository;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SimulationService}.
 *
 * I focused especially on the lifecycle state machine (INICIADA → FINALIZADA/CANCELADA),
 * because that's the most critical business logic in this service.
 * All repositories are mocked — no Spring context, no DB, ultra-fast!
 */
@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    // =========================================================
    // MOCKS
    // =========================================================
    @Mock
    private SimulationRepository simulationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClinicalScenarioRepository scenarioRepository;

    @Mock
    private SimulationEngineService simulationEngineService;

    @Mock
    private com.grupo3aor.innovationlab.repository.AlertRepository alertRepository;

    @Mock
    private RuleEvaluatorService ruleEvaluatorService;

    @InjectMocks
    private SimulationService simulationService;

    // =========================================================
    // TEST DATA FIXTURES
    // =========================================================
    private User mockUser;
    private ClinicalScenario mockScenario;
    private SimulationRequest validRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("nurse@vitalsim.pt")
                .firstName("Nurse")
                .lastName("Test")
                .passwordHash("hashed")
                .build();

        mockScenario = ClinicalScenario.builder()
                .id(1L)
                .name("Cardiac Arrest Scenario")
                .build();

        validRequest = SimulationRequest.builder()
                .scenarioId(1L)
                .build();
    }

    // =========================================================
    // startSimulation() TESTS
    // =========================================================

    @Test
    @DisplayName("startSimulation: deve criar simulação com status INICIADA e startedAt preenchido")
    void startSimulation_shouldCreateWithCorrectStatusAndTimestamp() {
        // ARRANGE
        when(userRepository.findByEmail("nurse@vitalsim.pt")).thenReturn(Optional.of(mockUser));
        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(mockScenario));

        UUID generatedId = UUID.randomUUID();
        Simulation savedSim = Simulation.builder()
                .id(generatedId)
                .scenario(mockScenario)
                .user(mockUser)
                .status(SimulationStatus.INICIADA)
                .startedAt(LocalDateTime.now())
                .build();
        when(simulationRepository.save(any(Simulation.class))).thenReturn(savedSim);

        // ACT
        SimulationResponse response = simulationService.startSimulation(validRequest, "nurse@vitalsim.pt");

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(generatedId);
        assertThat(response.getStatus()).isEqualTo(SimulationStatus.INICIADA);
        assertThat(response.getStartedAt()).isNotNull();
        assertThat(response.getUserEmail()).isEqualTo("nurse@vitalsim.pt");
        assertThat(response.getScenarioId()).isEqualTo(1L);

        verify(simulationRepository, times(1)).save(any(Simulation.class));
    }

    @Test
    @DisplayName("startSimulation: deve lançar exceção se o utilizador autenticado não existir")
    void startSimulation_shouldThrowIllegalArgument_whenUserNotFound() {
        when(userRepository.findByEmail("ghost@vitalsim.pt")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> simulationService.startSimulation(validRequest, "ghost@vitalsim.pt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user not found");

        verify(simulationRepository, never()).save(any());
    }

    @Test
    @DisplayName("startSimulation: deve lançar ResourceNotFoundException se o cenário não existir")
    void startSimulation_shouldThrowResourceNotFound_whenScenarioNotFound() {
        when(userRepository.findByEmail("nurse@vitalsim.pt")).thenReturn(Optional.of(mockUser));
        when(scenarioRepository.findById(99L)).thenReturn(Optional.empty());

        SimulationRequest badRequest = SimulationRequest.builder().scenarioId(99L).build();

        assertThatThrownBy(() -> simulationService.startSimulation(badRequest, "nurse@vitalsim.pt"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Clinical Scenario not found");

        verify(simulationRepository, never()).save(any());
    }

    // =========================================================
    // stopSimulation() TESTS — LIFECYCLE STATE MACHINE
    // =========================================================

    @Test
    @DisplayName("stopSimulation: deve mudar status para FINALIZADA e preencher endedAt")
    void stopSimulation_shouldSetFinalizedStatusAndEndTime() {
        // ARRANGE
        UUID simId = UUID.randomUUID();
        Simulation runningSim = Simulation.builder()
                .id(simId)
                .scenario(mockScenario)
                .user(mockUser)
                .status(SimulationStatus.INICIADA)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(simulationRepository.findById(simId)).thenReturn(Optional.of(runningSim));
        when(simulationRepository.save(any(Simulation.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        SimulationResponse response = simulationService.stopSimulation(simId, null);

        // ASSERT
        assertThat(response.getStatus()).isEqualTo(SimulationStatus.FINALIZADA);
        assertThat(response.getEndedAt()).isNotNull();
    }

    @Test
    @DisplayName("stopSimulation: deve lançar IllegalStateException se a simulação já estiver FINALIZADA")
    void stopSimulation_shouldThrowIllegalState_whenAlreadyFinalized() {
        // ARRANGE — I want to verify the guard clause works correctly
        UUID simId = UUID.randomUUID();
        Simulation finishedSim = Simulation.builder()
                .id(simId)
                .scenario(mockScenario)
                .user(mockUser)
                .status(SimulationStatus.FINALIZADA)
                .startedAt(LocalDateTime.now().minusHours(1))
                .endedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(simulationRepository.findById(simId)).thenReturn(Optional.of(finishedSim));

        // ACT & ASSERT
        assertThatThrownBy(() -> simulationService.stopSimulation(simId, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already finalized or canceled");

        verify(simulationRepository, never()).save(any());
    }

    @Test
    @DisplayName("stopSimulation: deve lançar IllegalStateException se a simulação já estiver CANCELADA")
    void stopSimulation_shouldThrowIllegalState_whenAlreadyCanceled() {
        // ARRANGE
        UUID simId = UUID.randomUUID();
        Simulation canceledSim = Simulation.builder()
                .id(simId)
                .scenario(mockScenario)
                .user(mockUser)
                .status(SimulationStatus.CANCELADA)
                .startedAt(LocalDateTime.now().minusHours(2))
                .endedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(simulationRepository.findById(simId)).thenReturn(Optional.of(canceledSim));

        // ACT & ASSERT
        assertThatThrownBy(() -> simulationService.stopSimulation(simId, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already finalized or canceled");

        verify(simulationRepository, never()).save(any());
    }

    @Test
    @DisplayName("stopSimulation: deve lançar ResourceNotFoundException para ID inexistente")
    void stopSimulation_shouldThrowResourceNotFound_whenSimulationNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(simulationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> simulationService.stopSimulation(nonExistentId, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Simulation not found");
    }

    // =========================================================
    // cancelSimulation() TESTS
    // =========================================================

    @Test
    @DisplayName("cancelSimulation: deve mudar status para CANCELADA e preencher endedAt")
    void cancelSimulation_shouldSetCanceledStatusAndEndTime() {
        // ARRANGE
        UUID simId = UUID.randomUUID();
        Simulation runningSim = Simulation.builder()
                .id(simId)
                .scenario(mockScenario)
                .user(mockUser)
                .status(SimulationStatus.EM_CURSO)
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(simulationRepository.findById(simId)).thenReturn(Optional.of(runningSim));
        when(simulationRepository.save(any(Simulation.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        SimulationResponse response = simulationService.cancelSimulation(simId);

        // ASSERT
        assertThat(response.getStatus()).isEqualTo(SimulationStatus.CANCELADA);
        assertThat(response.getEndedAt()).isNotNull();
    }

    @Test
    @DisplayName("cancelSimulation: deve lançar IllegalStateException se a simulação já estiver FINALIZADA")
    void cancelSimulation_shouldThrowIllegalState_whenAlreadyFinalized() {
        UUID simId = UUID.randomUUID();
        Simulation finishedSim = Simulation.builder()
                .id(simId)
                .scenario(mockScenario)
                .user(mockUser)
                .status(SimulationStatus.FINALIZADA)
                .startedAt(LocalDateTime.now().minusHours(1))
                .endedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(simulationRepository.findById(simId)).thenReturn(Optional.of(finishedSim));

        assertThatThrownBy(() -> simulationService.cancelSimulation(simId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already finalized or canceled");

        verify(simulationRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelSimulation: deve lançar IllegalStateException se a simulação já estiver CANCELADA")
    void cancelSimulation_shouldThrowIllegalState_whenAlreadyCanceled() {
        UUID simId = UUID.randomUUID();
        Simulation canceledSim = Simulation.builder()
                .id(simId)
                .scenario(mockScenario)
                .user(mockUser)
                .status(SimulationStatus.CANCELADA)
                .startedAt(LocalDateTime.now().minusHours(2))
                .endedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(simulationRepository.findById(simId)).thenReturn(Optional.of(canceledSim));

        assertThatThrownBy(() -> simulationService.cancelSimulation(simId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already finalized or canceled");

        verify(simulationRepository, never()).save(any());
    }

    // =========================================================
    // getHistory() TESTS
    // =========================================================

    @Test
    @DisplayName("getHistory: deve retornar lista com todas as simulações")
    void getHistory_shouldReturnAllSimulations() {
        // ARRANGE
        Simulation sim1 = Simulation.builder()
                .id(UUID.randomUUID())
                .scenario(mockScenario)
                .user(mockUser)
                .status(SimulationStatus.FINALIZADA)
                .startedAt(LocalDateTime.now().minusHours(2))
                .endedAt(LocalDateTime.now().minusHours(1))
                .build();
        Simulation sim2 = Simulation.builder()
                .id(UUID.randomUUID())
                .scenario(mockScenario)
                .user(mockUser)
                .status(SimulationStatus.CANCELADA)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .endedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(simulationRepository.findAll()).thenReturn(List.of(sim1, sim2));

        // ACT
        List<SimulationResponse> result = simulationService.getHistory();

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SimulationResponse::getStatus)
                .containsExactly(SimulationStatus.FINALIZADA, SimulationStatus.CANCELADA);
    }

    @Test
    @DisplayName("getHistory: deve retornar lista vazia quando não há simulações")
    void getHistory_shouldReturnEmptyList_whenNoSimulations() {
        when(simulationRepository.findAll()).thenReturn(List.of());

        List<SimulationResponse> result = simulationService.getHistory();

        assertThat(result).isEmpty();
    }
}
