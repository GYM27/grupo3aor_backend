package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.EvaluationReport;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.enums.Severity;
import com.grupo3aor.innovationlab.dto.EvaluationReportDTO;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.repository.EvaluationReportRepository;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationReportServiceTest {

    @Mock
    private EvaluationReportRepository repository;

    @Mock
    private SimulationMapper mapper;

    @Mock
    private SimulationRepository simulationRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private PhysiologicalReadingRepository readingRepository;

    @InjectMocks
    private EvaluationReportService service;

    private UUID simId;
    private Simulation mockSimulation;
    private EvaluationReportDTO mockRequestDto;
    private EvaluationReport mockReport;
    private EvaluationReportDTO mockResponseDto;

    @BeforeEach
    void setUp() {
        simId = UUID.randomUUID();
        mockSimulation = Simulation.builder().id(simId).startedAt(LocalDateTime.now().minusMinutes(10)).build();

        mockRequestDto = new EvaluationReportDTO();
        mockRequestDto.setSimulationId(simId);
        mockRequestDto.setRationaleText("Some text");

        mockReport = EvaluationReport.builder()
                .id(UUID.randomUUID())
                .simulation(mockSimulation)
                .rationaleText("Some text")
                .build();

        mockResponseDto = new EvaluationReportDTO();
        mockResponseDto.setId(mockReport.getId());
        mockResponseDto.setSimulationId(simId);
        mockResponseDto.setRationaleText("Some text");
    }

    @Test
    @DisplayName("saveReport: should save report and generate PDF")
    void saveReport_shouldSaveAndGeneratePDF() {
        when(simulationRepository.findById(simId)).thenReturn(Optional.of(mockSimulation));
        when(mapper.toEntity(any(EvaluationReportDTO.class), any(Simulation.class))).thenReturn(mockReport);
        
        PhysiologicalReading firstReading = new PhysiologicalReading();
        firstReading.setTimestamp(LocalDateTime.now().minusMinutes(5));
        PhysiologicalReading lastReading = new PhysiologicalReading();
        lastReading.setTimestamp(LocalDateTime.now());
        
        when(readingRepository.findFirstBySimulation_IdOrderByTimestampAsc(simId)).thenReturn(firstReading);
        when(readingRepository.findFirstBySimulation_IdOrderByTimestampDesc(simId)).thenReturn(lastReading);
        
        Rule mockRule = Rule.builder().name("Test Rule").severity(Severity.ALERTA).expressionDsl("{\"metric\":\"HR\",\"operator\":\">\",\"threshold\":100}").build();
        Alert mockAlert = Alert.builder().id(UUID.randomUUID()).rule(mockRule).valueAtTrigger(110.0).timestamp(LocalDateTime.now()).build();
        when(alertRepository.findBySimulation_Id(simId)).thenReturn(List.of(mockAlert));
        
        when(repository.save(any(EvaluationReport.class))).thenReturn(mockReport);
        when(mapper.toDto(any(EvaluationReport.class))).thenReturn(mockResponseDto);

        EvaluationReportDTO result = service.saveReport(mockRequestDto, "admin@test.com", "127.0.0.1");

        assertThat(result.getId()).isEqualTo(mockReport.getId());
        verify(repository).save(any(EvaluationReport.class));
        assertThat(mockReport.getPdfContent()).isNotNull();
    }

    @Test
    @DisplayName("saveReport: should throw when simulation not found")
    void saveReport_shouldThrowWhenSimNotFound() {
        when(simulationRepository.findById(simId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveReport(mockRequestDto, "admin@test.com", "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("getRawReportById: should return report")
    void getRawReportById_shouldReturnReport() {
        UUID reportId = mockReport.getId();
        when(repository.findById(reportId)).thenReturn(Optional.of(mockReport));

        EvaluationReport result = service.getRawReportById(reportId);

        assertThat(result.getId()).isEqualTo(reportId);
    }

    @Test
    @DisplayName("getRawReportById: should throw when not found")
    void getRawReportById_shouldThrowWhenNotFound() {
        UUID reportId = UUID.randomUUID();
        when(repository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRawReportById(reportId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Evaluation report not found");
    }
}
