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
        // Establecemos un messagebroker que se va a encargar de transmitir mensajes a traves de la comunicacion web socket
        config.enableSimpleBroker("/topic");
        // Definimos el prefijo de socket
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Establece endpoint ws-kitchen para la comunicacion socket
        registry.addEndpoint("/ws-kitchen")
                .setAllowedOriginPatterns("*") // CORS se maneja a traves de CorsConfig SecurityConfig
                .withSockJS();
    }
}