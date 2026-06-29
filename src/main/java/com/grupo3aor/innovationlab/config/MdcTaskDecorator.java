package com.grupo3aor.innovationlab.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * A task decorator that propagates the Mapped Diagnostic Context (MDC) 
 * from the main thread to asynchronous execution threads.
 * This guarantees that contextual logging information (like user and IP) 
 * is not lost when operations are executed via {@code @Async}.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Grab the MDC context from the original thread (the main one)
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        
        return () -> {
            try {
                // Copy it over to the current async thread
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                } else {
                    MDC.clear();
                }
                
                // Run the actual task
                runnable.run();
                
            } finally {
                // Clean it up at the end so we don't pollute the thread pool
                MDC.clear();
            }
        };
    }
}
