package com.grupo3aor.innovationlab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupo3aor.innovationlab.domain.entity.ClinicalScenario;
import com.grupo3aor.innovationlab.dto.ClinicalScenarioRequest;
import com.grupo3aor.innovationlab.dto.ClinicalScenarioResponse;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.repository.ClinicalScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalScenarioServiceTest {

    @Mock
    private ClinicalScenarioRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ClinicalScenarioService service;

    private ClinicalScenario mockScenario;
    private ClinicalScenarioRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockScenario = ClinicalScenario.builder()
                .id(1L)
                .name("Test Scenario")
                .description("Desc")
                .active(true)
                .build();

        mockRequest = ClinicalScenarioRequest.builder()
                .name("New Scenario")
                .description("New Desc")
                .build();
    }

    @Test
    @DisplayName("createScenario: should create and return response")
    void createScenario_shouldCreate() throws Exception {
        when(repository.save(any(ClinicalScenario.class))).thenReturn(mockScenario);

        ClinicalScenarioResponse result = service.createScenario(mockRequest, "admin@test.com", "127.0.0.1");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Scenario");
        verify(repository).save(any(ClinicalScenario.class));
    }

    @Test
    @DisplayName("getAllActiveScenarios: should return list")
    void getAllActiveScenarios_shouldReturnList() {
        when(repository.findAllByActiveTrue()).thenReturn(List.of(mockScenario));

        List<ClinicalScenarioResponse> results = service.getAllActiveScenarios();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("deleteScenario: should soft delete")
    void deleteScenario_shouldSoftDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockScenario));

        service.deleteScenario(1L);

        assertThat(mockScenario.isActive()).isFalse();
    }

    @Test
    @DisplayName("deleteScenario: should throw when not found")
    void deleteScenario_shouldThrowWhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteScenario(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getScenarioById: should return response")
    void getScenarioById_shouldReturnResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockScenario));

        ClinicalScenarioResponse result = service.getScenarioById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("updateScenario: should update and return response")
    void updateScenario_shouldUpdate() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockScenario));

        ClinicalScenarioResponse result = service.updateScenario(1L, mockRequest);

        assertThat(mockScenario.getName()).isEqualTo("New Scenario");
        assertThat(mockScenario.getDescription()).isEqualTo("New Desc");
        assertThat(result.getName()).isEqualTo("New Scenario");
    }
}
