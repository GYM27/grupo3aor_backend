package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalSystem;
import com.grupo3aor.innovationlab.dto.PhysiologicalSystemRequest;
import com.grupo3aor.innovationlab.dto.PhysiologicalSystemResponse;
import com.grupo3aor.innovationlab.exception.ResourceNotFoundException;
import com.grupo3aor.innovationlab.repository.PhysiologicalSystemRepository;
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
class PhysiologicalSystemServiceTest {

    @Mock
    private PhysiologicalSystemRepository repository;

    @InjectMocks
    private PhysiologicalSystemService service;

    private PhysiologicalSystem mockSystem;
    private PhysiologicalSystemRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockSystem = PhysiologicalSystem.builder()
                .id(1L)
                .systemName("Cardiovascular")
                .active(true)
                .build();

        mockRequest = PhysiologicalSystemRequest.builder()
                .systemName("Cardiovascular")
                .build();
    }

    @Test
    @DisplayName("createSystem: should create and return response")
    void createSystem_shouldCreate() {
        when(repository.findBySystemName(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(PhysiologicalSystem.class))).thenReturn(mockSystem);

        PhysiologicalSystemResponse result = service.createSystem(mockRequest, "admin@test.com", "127.0.0.1");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSystemName()).isEqualTo("Cardiovascular");
        verify(repository).save(any(PhysiologicalSystem.class));
    }

    @Test
    @DisplayName("createSystem: should throw if name exists")
    void createSystem_shouldThrowIfExists() {
        when(repository.findBySystemName(anyString())).thenReturn(Optional.of(mockSystem));

        assertThatThrownBy(() -> service.createSystem(mockRequest, "admin@test.com", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getAllActiveSystems: should return list")
    void getAllActiveSystems_shouldReturnList() {
        when(repository.findAllByActiveTrue()).thenReturn(List.of(mockSystem));

        List<PhysiologicalSystemResponse> results = service.getAllActiveSystems();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("deleteSystem: should delete")
    void deleteSystem_shouldDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockSystem));

        service.deleteSystem(1L);

        verify(repository).delete(mockSystem);
    }

    @Test
    @DisplayName("deleteSystem: should throw when not found")
    void deleteSystem_shouldThrowWhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSystem(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getSystemById: should return response")
    void getSystemById_shouldReturnResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockSystem));

        PhysiologicalSystemResponse result = service.getSystemById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("updateSystem: should update and return response")
    void updateSystem_shouldUpdate() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockSystem));
        
        PhysiologicalSystemRequest updateRequest = PhysiologicalSystemRequest.builder()
                .systemName("Respiratory")
                .build();
                
        when(repository.findBySystemName("Respiratory")).thenReturn(Optional.empty());
        when(repository.save(any(PhysiologicalSystem.class))).thenReturn(mockSystem);

        PhysiologicalSystemResponse result = service.updateSystem(1L, updateRequest);

        verify(repository).save(any(PhysiologicalSystem.class));
    }
    
    @Test
    @DisplayName("updateSystem: should throw if new name exists")
    void updateSystem_shouldThrowIfNewNameExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockSystem));
        
        PhysiologicalSystemRequest updateRequest = PhysiologicalSystemRequest.builder()
                .systemName("Respiratory")
                .build();
                
        when(repository.findBySystemName("Respiratory")).thenReturn(Optional.of(PhysiologicalSystem.builder().id(2L).build()));

        assertThatThrownBy(() -> service.updateSystem(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class);
                
        verify(repository, never()).save(any());
    }
}
