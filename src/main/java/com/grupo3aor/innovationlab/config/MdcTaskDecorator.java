package com.grupo3aor.innovationlab.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * I created this decorator to grab the MDC context from the main thread and pass it down 
 * to the async threads. Without this, we'd lose our user and IP info in the logs whenever 
 * we use @Async!
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
