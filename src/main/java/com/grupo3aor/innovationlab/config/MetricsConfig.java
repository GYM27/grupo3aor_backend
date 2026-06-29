package com.grupo3aor.innovationlab.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class for application metrics and telemetry.
 * It sets up the necessary infrastructure to collect execution data using Micrometer.
 */
@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {

    /**
     * Instantiates the {@link TimedAspect} bean to allow methods annotated with {@code @Timed}
     * to be intercepted by Micrometer, recording their execution time metrics.
     *
     * @param registry the {@link MeterRegistry} to record metrics into
     * @return the configured {@link TimedAspect}
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
