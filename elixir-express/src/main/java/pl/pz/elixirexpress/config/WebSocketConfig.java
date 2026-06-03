package pl.pz.elixirexpress.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Konfiguracja brokera wiadomości.
     * /topic — kanał na który serwer pushuje wiadomości do GUI (pub/sub)
     * /app  — prefix dla wiadomości wysyłanych z GUI do serwera (@MessageMapping)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    /**
     * Endpoint do nawiązania połączenia WebSocket.
     * GUI łączy się przez: ws://localhost:8083/ws
     * withSockJS() — fallback dla przeglądarek bez natywnego WebSocket
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}