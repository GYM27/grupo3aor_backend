package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import com.grupo3aor.innovationlab.domain.enums.Severity;
import com.grupo3aor.innovationlab.dto.AlertDTO;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.mapper.SimulationMapper;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.repository.RuleRepository;
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
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private SimulationMapper mapper;

    @Mock
    private SimulationRepository simulationRepository;

    @Mock
    private RuleRepository ruleRepository;

    @InjectMocks
    private AlertService alertService;

    private UUID simId;
    private UUID ruleId;
    private UUID alertId;
    private Simulation mockSimulation;
    private Rule mockRule;
    private Alert mockAlert;
    private AlertDTO mockAlertDTO;

    @BeforeEach
    void setUp() {
        simId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        alertId = UUID.randomUUID();

        mockSimulation = Simulation.builder().id(simId).build();
        mockRule = Rule.builder().id(ruleId).build();

        mockAlert = Alert.builder()
                .id(alertId)
                .simulation(mockSimulation)
                .rule(mockRule)
                .status(AlertStatus.ATIVO)
                .valueAtTrigger(150.0)
                .build();

        mockAlertDTO = new AlertDTO();
        mockAlertDTO.setId(alertId);
        mockAlertDTO.setSimulationId(simId);
        mockAlertDTO.setRuleId(ruleId);
        mockAlertDTO.setSeverity(Severity.ALERTA.name());
        mockAlertDTO.setStatus(AlertStatus.ATIVO);
        mockAlertDTO.setValueAtTrigger(150.0);
    }

    @Test
    @DisplayName("triggerAlert: should trigger alert and return dto")
    void triggerAlert_shouldReturnDto() {
        when(simulationRepository.findById(simId)).thenReturn(Optional.of(mockSimulation));
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(mockRule));
        when(mapper.toEntity(any(AlertDTO.class), any(Simulation.class), any(Rule.class))).thenReturn(mockAlert);
        when(alertRepository.save(any(Alert.class))).thenReturn(mockAlert);
        when(mapper.toDto(any(Alert.class))).thenReturn(mockAlertDTO);

        AlertDTO result = alertService.triggerAlert(mockAlertDTO, "user@test.com", "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(alertId);
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    @DisplayName("triggerAlert: should throw exception when simulation not found")
    void triggerAlert_shouldThrowWhenSimNotFound() {
        when(simulationRepository.findById(simId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.triggerAlert(mockAlertDTO, "user@test.com", "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Simulation not found");

        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("triggerAlert: should throw exception when rule not found")
    void triggerAlert_shouldThrowWhenRuleNotFound() {
        when(simulationRepository.findById(simId)).thenReturn(Optional.of(mockSimulation));
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.triggerAlert(mockAlertDTO, "user@test.com", "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rule not found");

        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: should update status and return dto")
    void updateStatus_shouldReturnDto() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(mockAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(mockAlert);
        
        AlertDTO updatedDTO = new AlertDTO();
        updatedDTO.setId(alertId);
        updatedDTO.setStatus(AlertStatus.RESOLVIDO);
        when(mapper.toDto(any(Alert.class))).thenReturn(updatedDTO);

        AlertDTO result = alertService.updateStatus(alertId, AlertStatus.RESOLVIDO, "user@test.com");

        assertThat(result.getStatus()).isEqualTo(AlertStatus.RESOLVIDO);
        verify(alertRepository).save(mockAlert);
        assertThat(mockAlert.getStatus()).isEqualTo(AlertStatus.RESOLVIDO);
    }

    @Test
    @DisplayName("updateStatus: should throw exception when alert not found")
    void updateStatus_shouldThrowWhenAlertNotFound() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.updateStatus(alertId, AlertStatus.RESOLVIDO, "user@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Target system alert context not found");

        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAlertsBySimulation: should return list of alerts")
    void getAlertsBySimulation_shouldReturnList() {
        when(alertRepository.findBySimulation_Id(simId)).thenReturn(List.of(mockAlert));
        when(mapper.toDto(any(Alert.class))).thenReturn(mockAlertDTO);

        List<AlertDTO> results = alertService.getAlertsBySimulation(simId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(alertId);
    }
}
