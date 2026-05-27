package pl.pz.elixir.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SorbnetResponseConsumer {

    @KafkaListener(topics = "responses.elixir", groupId = "elixir-group")
    public void consume(String message) {
        System.out.println(">>> ELIXIR response from SORBNET: " + message);
    }
}