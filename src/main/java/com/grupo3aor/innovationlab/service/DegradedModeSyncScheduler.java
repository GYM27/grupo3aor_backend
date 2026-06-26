package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DegradedModeSyncScheduler {

    private final DegradedModeBufferService bufferService;
    private final PhysiologicalReadingRepository readingRepository;
    private final AlertRepository alertRepository;

    // Running a sweep every 10 seconds to check if the DB is back online
    @Scheduled(fixedDelay = 10000)
    public void synchronizeData() {
        if (!bufferService.isDegraded() && bufferService.isBufferEmpty()) {
            return; // Everything is healthy, nothing to do here.
        }

        try {
            // 1. Connection Ping: We run a simple count query to verify the DB is actually accessible
            readingRepository.count();

            log.info("[MODO DEGRADADO] Connection restored! Initiating bulk synchronization...");

            // 2. Synchronizing Readings in batches of 1000
            while (true) {
                List<PhysiologicalReading> batch = bufferService.drainReadings(1000);
                if (batch.isEmpty()) break;
                
                try {
                    readingRepository.saveAll(batch); 
                    log.info("[MODO DEGRADADO] Synchronized batch of {} readings.", batch.size());
                } catch (Exception e) {
                    log.error("[CRÍTICO] Database failed again mid-sync! Reverting readings to the buffer to prevent data loss.");
                    bufferService.revertReadings(batch);
                    return; // Abort the sync process entirely; we'll try again in 10 seconds.
                }
            }

            // 3. Synchronizing Alerts in batches of 1000
            while (true) {
                List<Alert> alertBatch = bufferService.drainAlerts(1000);
                if (alertBatch.isEmpty()) break;
                
                try {
                    alertRepository.saveAll(alertBatch);
                    log.info("[MODO DEGRADADO] Synchronized batch of {} alerts.", alertBatch.size());
                } catch (Exception e) {
                    log.error("[CRÍTICO] Database failed again mid-sync! Reverting alerts to the buffer to prevent data loss.");
                    bufferService.revertAlerts(alertBatch);
                    return; // Abort sync
                }
            }

            // 4. If we reach this point, the sync was flawless. We can safely exit Degraded Mode.
            bufferService.setDegraded(false);
            log.info("[MODO DEGRADADO] Synchronization complete. System is 100% operational.");

        } catch (Exception e) {
            log.debug("[MODO DEGRADADO] Database is still inaccessible. Waiting for recovery...");
        }
    }
}