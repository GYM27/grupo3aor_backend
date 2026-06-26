package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.domain.entity.PhysiologicalReading;
import com.grupo3aor.innovationlab.domain.entity.Alert;
import com.grupo3aor.innovationlab.repository.PhysiologicalReadingRepository;
import com.grupo3aor.innovationlab.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataPersistenceComponent {

    private final PhysiologicalReadingRepository repository;
    private final AlertRepository alertRepository;

    // Strictly for chaos engineering and demos. 
    // Toggling this will forcefully trip the circuit breaker and simulate a total database outage.
    private boolean simulateDbFailure = false;

    public void setSimulateDbFailure(boolean simulate) {
        this.simulateDbFailure = simulate;
    }

    public boolean isSimulateDbFailure() {
        return this.simulateDbFailure;
    }
    // ----------------------

    // REQUIRES_NEW garante que esta transação é independente. 
    // Se falhar, não faz rollback da transação de quem o chamou.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PhysiologicalReading saveReadingSafely(PhysiologicalReading reading) {
        if (simulateDbFailure) throw new RuntimeException("Simulated Database Failure for Degraded Mode Test!");
        return repository.save(reading);
    }

    // NOVO: Método isolado para guardar Alertas
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Alert saveAlertSafely(Alert alert) {
        if (simulateDbFailure) throw new RuntimeException("Simulated Database Failure for Degraded Mode Test!");
        return alertRepository.save(alert);
    }

    // NOVO: Método isolado para guardar Batches
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public java.util.List<PhysiologicalReading> saveAllReadingsSafely(java.util.List<PhysiologicalReading> readings) {
        if (simulateDbFailure) throw new RuntimeException("Simulated Database Failure for Degraded Mode Test!");
        return repository.saveAll(readings);
    }
}