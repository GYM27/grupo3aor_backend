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
        lenient().when(mockRule.getActivationPersistence()).thenReturn(0);

        mockReading = PhysiologicalReading.builder()
                .id(UUID.randomUUID())
                .simulation(mockSimulation)
                .handle("HR")
                .value(150.0)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("evaluateReading: should not trigger alert immediately due to persistence")
    void evaluateReading_shouldNotTriggerImmediately() throws Exception {
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(mockRule));
        when(mockRule.isTriggeredBy(eq("HR"), anyDouble())).thenReturn(true);
        // Persistence > 0
        when(mockRule.getActivationPersistence()).thenReturn(30);

        service.evaluateReading(mockReading);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("evaluateReadingsBatch: should ignore empty batch")
    void evaluateReadingsBatch_shouldNotEvaluateWhenEmpty() {
        service.evaluateReadingsBatch(List.of());
        verify(ruleRepository, never()).findByActiveTrue();
    }
}
