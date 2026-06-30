package com.grupo3aor.innovationlab.config;

import com.grupo3aor.innovationlab.service.GlobalSettingsService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DegradedModeHealthIndicator implements HealthIndicator {

    private final GlobalSettingsService globalSettingsService;

    public DegradedModeHealthIndicator(GlobalSettingsService globalSettingsService) {
        this.globalSettingsService = globalSettingsService;
    }

    @Override
    public Health health() {
        if (globalSettingsService.isDbFailed()) {
            return Health.outOfService()
                    .withDetail("DegradedMode", true)
                    .withDetail("reason", "Simulated Database Failure Activated")
                    .build();
        }
        
        return Health.up().build();
    }
}
