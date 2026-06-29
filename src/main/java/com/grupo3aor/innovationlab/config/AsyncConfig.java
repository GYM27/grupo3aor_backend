package com.grupo3aor.innovationlab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration class that enables and configures asynchronous execution support.
 * It defines custom thread pools for handling background tasks and high-throughput operations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configures a dedicated Thread Pool for high-throughput background tasks,
     * such as physiological reading ingestion (stream and batch).
     * <p>
     * Features a DiscardPolicy to drop incoming telemetry frames when the system
     * is under extreme load, rather than blocking the main application threads.
     * It also uses {@link MdcTaskDecorator} to propagate logging context to async threads.
     * </p>
     *
     * @return the configured {@link Executor} for telemetry operations
     */
    @Bean(name = "telemetryExecutor")
    public Executor telemetryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Telemetry-");
        
        // Discard policy: if the queue is full and all threads are busy, 
        // new tasks are silently discarded to prevent server crash or lag.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        
        // I added this decorator so we don't lose our MDC context (like User and IP) when switching threads!
        executor.setTaskDecorator(new MdcTaskDecorator());
        
        executor.initialize();
        return executor;
    }
}
