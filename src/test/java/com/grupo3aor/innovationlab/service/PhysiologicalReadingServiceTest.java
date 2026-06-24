package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
import com.grupo3aor.innovationlab.repository.SimulationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhysiologicalReadingServiceTest {

    @Mock
    private PhysiologicalReadingRepository repository;

    @Mock
    private SimulationMapper mapper;

    @Mock
    private SimulationRepository simulationRepository;

    @Mock
    private RuleEvaluatorService ruleEvaluatorService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PhysiologicalReadingService service;

    private UUID simId;
    private Simulation mockSimulation;
    private PhysiologicalReading mockReading;
    private PhysiologicalReadingDTO mockDto;

    @BeforeEach
    void setUp() {
        simId = UUID.randomUUID();
        mockSimulation = Simulation.builder().id(simId).build();
        
        mockReading = PhysiologicalReading.builder()
                .id(UUID.randomUUID())
                .simulation(mockSimulation)
                .build();
                
        mockDto = PhysiologicalReadingDTO.builder()
                .simulationId(simId)
                .build();
    }

    @Test
    @DisplayName("createReading: should create, evaluate and notify")
    void createReading_shouldCreateAndNotify() throws Exception {
        when(simulationRepository.findById(simId)).thenReturn(Optional.of(mockSimulation));
        when(mapper.toEntity(any(PhysiologicalReadingDTO.class), any(Simulation.class))).thenReturn(mockReading);
        when(repository.save(any(PhysiologicalReading.class))).thenReturn(mockReading);
        when(mapper.toDto(any(PhysiologicalReading.class))).thenReturn(mockDto);

        PhysiologicalReadingDTO result = service.createReading(mockDto, "admin@test.com", "127.0.0.1");

        assertThat(result).isNotNull();
        verify(repository).save(mockReading);
        verify(messagingTemplate).convertAndSend(eq("/topic/simulations/" + simId + "/readings"), any(PhysiologicalReadingDTO.class));
        verify(ruleEvaluatorService).evaluateReading(mockReading);
    }

    @Test
    @DisplayName("createReading: should throw when simulation not found")
    void createReading_shouldThrowWhenSimNotFound() {
        when(simulationRepository.findById(simId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createReading(mockDto, "admin@test.com", "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("createReadingBatch: should create all and evaluate in batch")
    void createReadingBatch_shouldCreateAndEvaluate() {
        when(simulationRepository.findById(simId)).thenReturn(Optional.of(mockSimulation));
        when(mapper.toEntity(any(PhysiologicalReadingDTO.class), any(Simulation.class))).thenReturn(mockReading);
        when(repository.saveAll(anyList())).thenReturn(List.of(mockReading));
        when(mapper.toDto(any(PhysiologicalReading.class))).thenReturn(mockDto);

        List<PhysiologicalReadingDTO> results = service.createReadingBatch(List.of(mockDto), "admin@test.com", "127.0.0.1");

        assertThat(results).hasSize(1);
        verify(repository).saveAll(anyList());
        verify(ruleEvaluatorService).evaluateReadingsBatch(anyList());
    }
    
    @Test
    @DisplayName("createReadingBatch: should return empty list when input is empty")
    void createReadingBatch_shouldReturnEmpty() {
        List<PhysiologicalReadingDTO> results = service.createReadingBatch(List.of(), "admin@test.com", "127.0.0.1");
        assertThat(results).isEmpty();
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("getReadingsBySimulation: should return list")
    void getReadingsBySimulation_shouldReturnList() {
        when(repository.findBySimulation_Id(simId)).thenReturn(List.of(mockReading));
        when(mapper.toDto(any(PhysiologicalReading.class))).thenReturn(mockDto);

        List<PhysiologicalReadingDTO> results = service.getReadingsBySimulation(simId);

        assertThat(results).hasSize(1);
    }
    
    @Test
    @DisplayName("createReadingAsync: should call createReading")
    void createReadingAsync_shouldWork() {
        when(simulationRepository.findById(simId)).thenReturn(Optional.of(mockSimulation));
        when(mapper.toEntity(any(PhysiologicalReadingDTO.class), any(Simulation.class))).thenReturn(mockReading);
        when(repository.save(any(PhysiologicalReading.class))).thenReturn(mockReading);
        
        service.createReadingAsync(mockDto, "admin@test.com", "127.0.0.1");
        
        verify(repository).save(any(PhysiologicalReading.class));
    }
    
    @Test
    @DisplayName("createReadingBatchAsync: should call createReadingBatch")
    void createReadingBatchAsync_shouldWork() {
        when(simulationRepository.findById(simId)).thenReturn(Optional.of(mockSimulation));
        when(mapper.toEntity(any(PhysiologicalReadingDTO.class), any(Simulation.class))).thenReturn(mockReading);
        when(repository.saveAll(anyList())).thenReturn(List.of(mockReading));
        
        service.createReadingBatchAsync(List.of(mockDto), "admin@test.com", "127.0.0.1");
        
        verify(repository).saveAll(anyList());
    }
}
