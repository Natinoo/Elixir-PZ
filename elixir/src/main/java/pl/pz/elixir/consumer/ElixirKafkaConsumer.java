package pl.pz.elixir.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.pz.elixir.service.SessionService;

@Component
public class ElixirKafkaConsumer {

    private final SessionService sessionService;

    public ElixirKafkaConsumer(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @KafkaListener(topics = "payments.elixir", groupId = "elixir-group")
    public void consume(String message) {
        System.out.println(">>> ELIXIR received: " + message);

        sessionService.addToSession(message);
    }
}