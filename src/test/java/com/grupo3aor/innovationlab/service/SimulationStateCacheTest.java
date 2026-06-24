package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationStateCacheTest {

    private SimulationStateCache cache;
    private UUID simId;
    private PhysiologicalReading mockReading;

    @BeforeEach
    void setUp() {
        cache = new SimulationStateCache();
        simId = UUID.randomUUID();
        mockReading = new PhysiologicalReading();
    }

    @Test
    @DisplayName("addReading: should add reading to queue")
    void addReading_shouldAddReading() {
        cache.addReading(simId, mockReading);

        Queue<PhysiologicalReading> queue = cache.getReadings(simId);
        assertThat(queue).isNotNull();
        assertThat(queue).hasSize(1);
        assertThat(queue.peek()).isEqualTo(mockReading);
    }

    @Test
    @DisplayName("getReadings: should return null when simulation not cached")
    void getReadings_shouldReturnNullWhenNotCached() {
        Queue<PhysiologicalReading> queue = cache.getReadings(simId);
        assertThat(queue).isNull();
    }

    @Test
    @DisplayName("clearSimulationCache: should clear cache for simulation")
    void clearSimulationCache_shouldClear() {
        cache.addReading(simId, mockReading);
        assertThat(cache.getReadings(simId)).isNotNull();

        cache.clearSimulationCache(simId);

        assertThat(cache.getReadings(simId)).isNull();
    }
}
