package com.grupo3aor.innovationlab.service;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state manager that holds the sliding window of physiological data
 * for real-time analysis. Uses Guava's EvictingQueue to automatically drop
 * old readings and prevent OutOfMemory errors during continuous data streams.
 */
@Service
public class SimulationStateCache {

    // Limit of readings to keep in RAM per active simulation
    private static final int MAX_READINGS = 5000;

    // Thread-safe map mapping simulation IDs to Thread-safe EvictingQueues
    private final Map<UUID, Queue<PhysiologicalReading>> cache = new ConcurrentHashMap<>();

    /**
     * Adds a new physiological reading to the memory window.
     * If the queue reaches its maximum capacity, the oldest element is evicted.
     *
     * @param simulationId the simulation identifier
     * @param reading      the physiological reading to cache
     */
    public void addReading(UUID simulationId, PhysiologicalReading reading) {
        Queue<PhysiologicalReading> queue = cache.computeIfAbsent(
                simulationId, 
                id -> Queues.synchronizedQueue(EvictingQueue.create(MAX_READINGS))
        );
        queue.add(reading);
    }

    /**
     * Retrieves the current in-memory sliding window for the specified simulation.
     * Returns null if no cache exists.
     *
     * @param simulationId the simulation identifier
     * @return the queue of recent readings
     */
    public Queue<PhysiologicalReading> getReadings(UUID simulationId) {
        return cache.get(simulationId);
    }

    /**
     * Clears the cache for a simulation.
     * Should be called when a simulation is finalized or canceled.
     *
     * @param simulationId the simulation identifier
     */
    public void clearSimulationCache(UUID simulationId) {
        cache.remove(simulationId);
    }
}
