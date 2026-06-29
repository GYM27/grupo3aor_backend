package com.grupo3aor.innovationlab.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

/**
 * A filter that intercepts every HTTP request to capture the user's IP address
 * and email (if authenticated), and stores them in the Mapped Diagnostic Context (MDC).
 * This ensures that these details are automatically included in all application logs
 * for reliable audit trailing and debugging.
 */
@Component
@Slf4j
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String MDC_USER_KEY = "user";
    private static final String MDC_IP_KEY = "ip";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Let's grab the IP address first
            String ip = request.getRemoteAddr();
            if (ip != null) {
                MDC.put(MDC_IP_KEY, ip);
            }

            // Now let's figure out who is making the request
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                MDC.put(MDC_USER_KEY, auth.getName());
            } else {
                MDC.put(MDC_USER_KEY, "SYSTEM");
            }

            // I'm logging EVERY single request here so we have a bulletproof audit trail of ALL system activities!
            log.info("System activity: executed {} {}", request.getMethod(), request.getRequestURI());

            // Move along with the request!
            filterChain.doFilter(request, response);

        } finally {
            // Always clean up the MDC afterwards, otherwise we'll get memory leaks 
            // and wrong info popping up in other requests on the same thread
            MDC.remove(MDC_USER_KEY);
            MDC.remove(MDC_IP_KEY);
        }
    }
}
