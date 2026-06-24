package com.grupo3aor.innovationlab.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupo3aor.innovationlab.domain.entity.ClinicalScenario;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.enums.SimulationStatus;
import com.grupo3aor.innovationlab.dto.MetricDTO;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulationEngineServiceTest {

    @Mock
    private SimulationRepository simulationRepository;

    @Mock
    private PhysiologicalReadingService physiologicalReadingService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SimulationEngineService service;

    private UUID simId;
    private Simulation mockSimulation;

    @BeforeEach
    void setUp() {
        simId = UUID.randomUUID();
        mockSimulation = Simulation.builder()
                .id(simId)
                .status(SimulationStatus.INICIADA)
                .startedAt(LocalDateTime.now())
                .scenario(ClinicalScenario.builder().metricsPayload("[{}]").build())
                .build();
    }

    @Test
    @DisplayName("cancelOrphanSimulations: should cancel orphans")
    void cancelOrphanSimulations_shouldCancel() {
        when(simulationRepository.findAllByStatusIn(anyList())).thenReturn(List.of(mockSimulation));

        service.cancelOrphanSimulations();

        verify(simulationRepository).save(mockSimulation);
        assert mockSimulation.getStatus() == SimulationStatus.CANCELADA;
    }

    @Test
    @DisplayName("increment and decrement active simulations should work")
    void activeSimulations_shouldIncrementAndDecrement() {
        // Starts at 0
        service.generateContinuousData();
        verify(simulationRepository, never()).findAllByStatusIn(anyList());

        // Increment
        service.incrementActiveSimulations();
        when(simulationRepository.findAllByStatusIn(anyList())).thenReturn(List.of());
        service.generateContinuousData();
        verify(simulationRepository, times(1)).findAllByStatusIn(anyList());

        // Decrement
        service.decrementActiveSimulations();
        service.generateContinuousData();
        verify(simulationRepository, times(1)).findAllByStatusIn(anyList()); // Still 1 from before
    }
    
    @Test
    @DisplayName("generateContinuousData: should finalize when payload is empty")
    void generateContinuousData_shouldFinalizeWhenEmptyPayload() {
        mockSimulation.getScenario().setMetricsPayload("");
        service.incrementActiveSimulations();
        when(simulationRepository.findAllByStatusIn(anyList())).thenReturn(List.of(mockSimulation));
        
        service.generateContinuousData();
        
        verify(simulationRepository).save(mockSimulation);
        assert mockSimulation.getStatus() == SimulationStatus.FINALIZADA;
    }
    
    @Test
    @DisplayName("generateContinuousData: should finalize when metrics list is empty")
    void generateContinuousData_shouldFinalizeWhenEmptyMetrics() throws Exception {
        service.incrementActiveSimulations();
        when(simulationRepository.findAllByStatusIn(anyList())).thenReturn(List.of(mockSimulation));
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of());
        
        service.generateContinuousData();
        
        verify(simulationRepository).save(mockSimulation);
        assert mockSimulation.getStatus() == SimulationStatus.FINALIZADA;
    }
}
