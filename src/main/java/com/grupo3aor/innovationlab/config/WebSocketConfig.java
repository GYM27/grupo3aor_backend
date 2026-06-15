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
<<<<<<< HEAD
        // Habilita um broker na memória para enviar mensagens de volta para o frontend
        config.enableSimpleBroker("/topic");
        // Prefixo para mensagens que vêm do frontend para o backend (se precisarmos)
=======
        // Enable a simple memory-based message broker to carry the messages back to the client on destinations prefixed with "/topic"
        config.enableSimpleBroker("/topic");
        // Prefix for messages that are bound for methods annotated with @MessageMapping
>>>>>>> d9d57c2f9a3f3c82f444b2b421436c61df437317
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
<<<<<<< HEAD
        // I changed the endpoint to "/api/ws" so it routes perfectly through our React Vite Proxy.
        // I also kept setAllowedOriginPatterns("*") to ensure no CORS issues block the handshake.
        registry.addEndpoint("/api/ws")
                .setAllowedOriginPatterns("*")
=======
        // The endpoint that clients will use to connect to the WebSocket server
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174")
>>>>>>> d9d57c2f9a3f3c82f444b2b421436c61df437317
                .withSockJS();
    }
}
