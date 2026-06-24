package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalSystem;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.Simulation;
import com.grupo3aor.innovationlab.domain.enums.AlertStatus;
import com.grupo3aor.innovationlab.domain.enums.Severity;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEvaluatorServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RuleEvaluatorService service;

    private UUID simId;
    private UUID ruleId;
    private Simulation mockSimulation;
    private Rule mockRule;
    private PhysiologicalReading mockReading;

    @BeforeEach
    void setUp() {
        simId = UUID.randomUUID();
        ruleId = UUID.randomUUID();

        mockSimulation = Simulation.builder().id(simId).build();

        mockRule = mock(Rule.class);
        lenient().when(mockRule.getId()).thenReturn(ruleId);
        lenient().when(mockRule.getSeverity()).thenReturn(Severity.ALERTA);
        lenient().when(mockRule.getSystem()).thenReturn(PhysiologicalSystem.builder().systemName("Test System").build());
        lenient().when(mockRule.getPersistence()).thenReturn(0);

        mockReading = PhysiologicalReading.builder()
                .id(UUID.randomUUID())
                .simulation(mockSimulation)
                .handle("HR")
                .value(150.0)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("evaluateReading: should trigger alert when rule is triggered and no active alert exists")
    void evaluateReading_shouldTriggerAlert() throws Exception {
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(mockRule));
        when(mockRule.isTriggeredBy(eq("HR"), anyDouble())).thenReturn(true);
        when(alertRepository.existsBySimulationAndRuleAndStatus(mockSimulation, mockRule, AlertStatus.ATIVO)).thenReturn(false);

        service.evaluateReading(mockReading);

        verify(alertRepository).save(any(Alert.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/simulations/" + simId + "/alerts"), any(Map.class));
    }

    @Test
    @DisplayName("evaluateReading: should not trigger alert when rule is triggered but active alert already exists")
    void evaluateReading_shouldNotTriggerWhenActiveExists() throws Exception {
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(mockRule));
        when(mockRule.isTriggeredBy(eq("HR"), anyDouble())).thenReturn(true);
        when(alertRepository.existsBySimulationAndRuleAndStatus(mockSimulation, mockRule, AlertStatus.ATIVO)).thenReturn(true);

        service.evaluateReading(mockReading);

        verify(alertRepository, never()).save(any(Alert.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Map.class));
    }

    @Test
    @DisplayName("evaluateReading: should not trigger when rule is not triggered")
    void evaluateReading_shouldNotTriggerWhenRuleNotTriggered() throws Exception {
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(mockRule));
        when(mockRule.isTriggeredBy(eq("HR"), anyDouble())).thenReturn(false);

        service.evaluateReading(mockReading);

        verify(alertRepository, never()).existsBySimulationAndRuleAndStatus(any(), any(), any());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("evaluateReadingsBatch: should evaluate batch and save all new alerts")
    void evaluateReadingsBatch_shouldEvaluateAndSave() throws Exception {
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(mockRule));
        when(alertRepository.existsBySimulationAndRuleAndStatus(mockSimulation, mockRule, AlertStatus.ATIVO)).thenReturn(false);
        when(mockRule.isTriggeredBy(eq("HR"), anyDouble())).thenReturn(true);

        service.evaluateReadingsBatch(List.of(mockReading));

        verify(alertRepository).saveAll(anyList());
        verify(messagingTemplate).convertAndSend(eq("/topic/simulations/" + simId + "/alerts"), any(Map.class));
    }
    
    @Test
    @DisplayName("evaluateReadingsBatch: should not evaluate when readings list is empty")
    void evaluateReadingsBatch_shouldNotEvaluateWhenEmpty() {
        service.evaluateReadingsBatch(List.of());
        verify(ruleRepository, never()).findByActiveTrue();
    }
}
