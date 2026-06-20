package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalSystem;
import com.grupo3aor.innovationlab.domain.entity.Rule;
import com.grupo3aor.innovationlab.domain.entity.User;
import com.grupo3aor.innovationlab.domain.enums.Severity;
import com.grupo3aor.innovationlab.dto.RuleRequest;
import com.grupo3aor.innovationlab.dto.RuleResponse;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.repository.PhysiologicalSystemRepository;
import com.grupo3aor.innovationlab.repository.RuleRepository;
import com.grupo3aor.innovationlab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RuleService}.
 *
 * I used @ExtendWith(MockitoExtension.class) to run these tests purely with Mockito,
 * without starting the Spring context — making them blazing fast.
 * The Repositories are mocked so we never touch a real database here!
 */
@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    // =========================================================
    // MOCKS — these replace the real Spring beans
    // =========================================================
    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhysiologicalSystemRepository systemRepository;

    @InjectMocks
    private RuleService ruleService;

    // =========================================================
    // TEST DATA — reusable fixtures built before each test
    // =========================================================
    private User mockUser;
    private PhysiologicalSystem mockSystem;
    private RuleRequest validRequest;

    @BeforeEach
    void setUp() {
        // I built minimal but realistic fake objects so tests stay readable
        mockUser = User.builder()
                .id(1L)
                .email("admin@vitalsim.pt")
                .firstName("Admin")
                .lastName("Test")
                .passwordHash("hashed")
                .build();

        mockSystem = PhysiologicalSystem.builder()
                .id(1L)
                .systemName("Cardiovascular")
                .build();

        validRequest = RuleRequest.builder()
                .systemId(1L)
                .expressionDsl("IF heartRate > 150 THEN ALERTA")
                .severity(Severity.ALERTA)
                .build();
    }

    // =========================================================
    // createRule() TESTS
    // =========================================================

    @Test
    @DisplayName("createRule: deve criar e retornar a regra com os dados corretos")
    void createRule_shouldSaveAndReturnRule_whenValidInput() {
        // ARRANGE — configure what the mocked repositories should return
        when(userRepository.findByEmail("admin@vitalsim.pt")).thenReturn(Optional.of(mockUser));
        when(systemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));

        UUID generatedId = UUID.randomUUID();
        Rule savedRule = Rule.builder()
                .id(generatedId)
                .system(mockSystem)
                .expressionDsl(validRequest.getExpressionDsl())
                .severity(validRequest.getSeverity())
                .createdByUser(mockUser)
                .build();
        when(ruleRepository.save(any(Rule.class))).thenReturn(savedRule);

        // ACT
        RuleResponse response = ruleService.createRule(validRequest, "admin@vitalsim.pt");

        // ASSERT — verify the response contains the expected data
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(generatedId);
        assertThat(response.getExpressionDsl()).isEqualTo("IF heartRate > 150 THEN ALERTA");
        assertThat(response.getSeverity()).isEqualTo(Severity.ALERTA);
        assertThat(response.getSystemId()).isEqualTo(1L);
        assertThat(response.getCreatedByUserEmail()).isEqualTo("admin@vitalsim.pt");

        // I also verify that save() was called exactly once with the correct entity
        verify(ruleRepository, times(1)).save(any(Rule.class));
    }

    @Test
    @DisplayName("createRule: deve lançar exceção se o utilizador autenticado não existir na BD")
    void createRule_shouldThrowIllegalArgument_whenUserNotFound() {
        // ARRANGE — user does not exist in the database
        when(userRepository.findByEmail("ghost@vitalsim.pt")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> ruleService.createRule(validRequest, "ghost@vitalsim.pt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user not found");

        // I verify we never even attempted to save anything to the DB
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createRule: deve lançar ResourceNotFoundException se o sistema fisiológico não existir")
    void createRule_shouldThrowResourceNotFound_whenSystemNotFound() {
        // ARRANGE — user exists, but the system ID is invalid
        when(userRepository.findByEmail("admin@vitalsim.pt")).thenReturn(Optional.of(mockUser));
        when(systemRepository.findById(99L)).thenReturn(Optional.empty());

        RuleRequest badRequest = RuleRequest.builder()
                .systemId(99L)
                .expressionDsl("IF heartRate > 200 THEN CRITICO")
                .severity(Severity.CRITICO)
                .build();

        // ACT & ASSERT
        assertThatThrownBy(() -> ruleService.createRule(badRequest, "admin@vitalsim.pt"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Physiological System not found");

        verify(ruleRepository, never()).save(any());
    }

    // =========================================================
    // getAllActiveRules() TESTS
    // =========================================================

    @Test
    @DisplayName("getAllActiveRules: deve retornar lista mapeada de regras ativas")
    void getAllActiveRules_shouldReturnMappedList() {
        // ARRANGE
        Rule rule1 = Rule.builder()
                .id(UUID.randomUUID())
                .system(mockSystem)
                .expressionDsl("IF spo2 < 90 THEN CRITICO")
                .severity(Severity.CRITICO)
                .createdByUser(mockUser)
                .active(true)
                .build();
        Rule rule2 = Rule.builder()
                .id(UUID.randomUUID())
                .system(mockSystem)
                .expressionDsl("IF heartRate > 150 THEN ALERTA")
                .severity(Severity.ALERTA)
                .createdByUser(mockUser)
                .active(true)
                .build();

        when(ruleRepository.findAll()).thenReturn(List.of(rule1, rule2));

        // ACT
        List<RuleResponse> result = ruleService.getAllRules(null);

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result).extracting(RuleResponse::getSeverity)
                .containsExactly(Severity.CRITICO, Severity.ALERTA);
    }

    @Test
    @DisplayName("getAllActiveRules: deve retornar lista vazia quando não há regras ativas")
    void getAllActiveRules_shouldReturnEmptyList_whenNoActiveRules() {
        when(ruleRepository.findAll()).thenReturn(List.of());

        List<RuleResponse> result = ruleService.getAllRules(null);

        assertThat(result).isEmpty();
    }

    // =========================================================
    // deactivateRule() TESTS
    // =========================================================

    @Test
    @DisplayName("deactivateRule: deve chamar deleteById quando o ID existe")
    void deactivateRule_shouldCallDeleteById_whenRuleExists() {
        // ARRANGE
        UUID existingId = UUID.randomUUID();
        when(ruleRepository.existsById(existingId)).thenReturn(true);

        // ACT
        ruleService.deactivateRule(existingId);

        // ASSERT — verify the soft-delete mechanism was triggered
        verify(ruleRepository, times(1)).deleteById(existingId);
    }

    @Test
    @DisplayName("deactivateRule: deve lançar ResourceNotFoundException para ID inexistente")
    void deactivateRule_shouldThrowResourceNotFound_whenRuleDoesNotExist() {
        // ARRANGE
        UUID nonExistentId = UUID.randomUUID();
        when(ruleRepository.existsById(nonExistentId)).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> ruleService.deactivateRule(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rule not found");

        // I make sure deleteById was NEVER called for a non-existent rule
        verify(ruleRepository, never()).deleteById(any());
    }

    // =========================================================
    // updateRule() TESTS
    // =========================================================

    @Test
    @DisplayName("updateRule: deve preencher o campo 'updatedBy' com o email do operador (fix do bug #3)")
    void updateRule_shouldSetUpdatedBy_withOperatorEmail() {
        // ARRANGE
        UUID ruleId = UUID.randomUUID();
        Rule existingRule = Rule.builder()
                .id(ruleId)
                .system(mockSystem)
                .expressionDsl("OLD expression")
                .severity(Severity.ALERTA)
                .createdByUser(mockUser)
                .build();

        RuleRequest updateRequest = RuleRequest.builder()
                .systemId(1L)
                .expressionDsl("NEW improved expression")
                .severity(Severity.CRITICO)
                .build();

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(existingRule));
        when(systemRepository.findById(1L)).thenReturn(Optional.of(mockSystem));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        RuleResponse response = ruleService.updateRule(ruleId, updateRequest, "admin@vitalsim.pt");

        // ASSERT — the response should reflect the updated values
        assertThat(response.getExpressionDsl()).isEqualTo("NEW improved expression");
        assertThat(response.getSeverity()).isEqualTo(Severity.CRITICO);

        // I capture the saved entity to assert that updatedBy was actually filled in
        // This is the regression test for Bug #3: the field was being silently ignored!
        ArgumentCaptor<Rule> ruleCaptor = ArgumentCaptor.forClass(Rule.class);
        verify(ruleRepository).save(ruleCaptor.capture());
        assertThat(ruleCaptor.getValue().getUpdatedBy()).isEqualTo("admin@vitalsim.pt");
    }

    @Test
    @DisplayName("updateRule: deve lançar ResourceNotFoundException para ID de regra inexistente")
    void updateRule_shouldThrowResourceNotFound_whenRuleDoesNotExist() {
        // ARRANGE
        UUID nonExistentId = UUID.randomUUID();
        when(ruleRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> ruleService.updateRule(nonExistentId, validRequest, "admin@vitalsim.pt"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rule not found");

        verify(ruleRepository, never()).save(any());
    }
}
