package pl.pz.elixirexpress;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SorbnetResponseConsumer {

    @KafkaListener(topics = "responses.elixir-express", groupId = "elixir-express-group")
    public void consume(String message) {
        System.out.println(">>> ELIXIR-EXPRESS response from SORBNET: " + message);
    }
}