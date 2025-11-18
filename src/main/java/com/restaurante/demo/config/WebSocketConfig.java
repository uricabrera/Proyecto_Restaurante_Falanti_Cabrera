package com.restaurante.demo.config;

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
        // Enable a simple memory-based message broker to send messages back to the client on destinations prefixed with /topic
        config.enableSimpleBroker("/topic");
        // Defines the prefix for messages that are bound for methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws-kitchen" endpoint, enabling the SockJS protocol options.
        // Check-And-Act: Allowed Origin Patterns fixed per Audit 1 to avoid CORS issues with credentials
        registry.addEndpoint("/ws-kitchen")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
