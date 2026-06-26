package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class DegradedModeBufferService {

    // Atomic flag ensuring thread-safe status updates across our system
    private final AtomicBoolean isDegraded = new AtomicBoolean(false);

    // Strict capacity limits using LinkedBlockingQueue for safe O(1) operations during high loads
    private final LinkedBlockingQueue<PhysiologicalReading> pendingReadings = new LinkedBlockingQueue<>(50000);
    private final LinkedBlockingQueue<Alert> pendingAlerts = new LinkedBlockingQueue<>(10000);

    public boolean isDegraded() {
        return isDegraded.get();
    }

    public void setDegraded(boolean status) {
        isDegraded.set(status);
    }

    public void addPendingReading(PhysiologicalReading reading) {
        // Using offer() instead of add() prevents crashes if the queue is full.
        // If it's full, we simply drop the oldest reading to make room for the new one (Ring Buffer strategy).
        if (!pendingReadings.offer(reading)) {
            pendingReadings.poll(); // Evict the oldest
            pendingReadings.offer(reading); // Retry insertion
            log.warn("[MODO DEGRADADO] Limite de leituras atingido (50.000). A descartar dados antigos.");
        }
    }

    public void addPendingAlert(Alert alert) {
        if (!pendingAlerts.offer(alert)) {
            pendingAlerts.poll();
            pendingAlerts.offer(alert);
        }
    }

    // Draining the buffer safely in controlled batches
    public List<PhysiologicalReading> drainReadings(int batchSize) {
        List<PhysiologicalReading> batch = new ArrayList<>();
        pendingReadings.drainTo(batch, batchSize);
        return batch;
    }

    public List<Alert> drainAlerts(int batchSize) {
        List<Alert> batch = new ArrayList<>();
        pendingAlerts.drainTo(batch, batchSize);
        return batch;
    }

    public boolean isBufferEmpty() {
        return pendingReadings.isEmpty() && pendingAlerts.isEmpty();
    }
    
    // Safely restoring data back to the queue if our bulk save operation fails mid-flight
    public void revertReadings(List<PhysiologicalReading> batch) {
        batch.forEach(this::addPendingReading);
    }

    public void revertAlerts(List<Alert> batch) {
        batch.forEach(this::addPendingAlert);
    }
}