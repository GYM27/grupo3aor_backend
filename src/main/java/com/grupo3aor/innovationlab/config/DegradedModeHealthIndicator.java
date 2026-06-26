package com.grupo3aor.innovationlab.config;

import com.grupo3aor.innovationlab.service.DegradedModeBufferService;
import com.grupo3aor.innovationlab.service.DataPersistenceComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DegradedModeHealthIndicator implements HealthIndicator {

    private final DegradedModeBufferService bufferService;
    private final DataPersistenceComponent dataPersistenceComponent;

    @Override
    public Health health() {
        // Checking both the real buffer state and our manual chaos flag. 
        // Tripping either one means the actuator should immediately broadcast an OUT_OF_SERVICE status.
        if (bufferService.isDegraded() || dataPersistenceComponent.isSimulateDbFailure()) {
            return Health.outOfService().withDetail("reason", "Modo Degradado Ativo - Falha na Base de Dados").build();
        }
        return Health.up().build();
    }
}
