package com.grupo3aor.innovationlab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita um broker na memória para enviar mensagens de volta para o frontend
        config.enableSimpleBroker("/topic");
        // Prefixo para mensagens que vêm do frontend para o backend (se precisarmos)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // I changed the endpoint to "/api/ws" so it routes perfectly through our React Vite Proxy.
        // I also kept setAllowedOriginPatterns("*") to ensure no CORS issues block the handshake.
        registry.addEndpoint("/api/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
