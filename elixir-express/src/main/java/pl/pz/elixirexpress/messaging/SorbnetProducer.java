package pl.pz.elixirexpress;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SorbnetProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public SorbnetProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendToSorbnet(String payload) {
        String key = UUID.randomUUID().toString();
        kafkaTemplate.send("payments.express.sorbnet", key, payload);
        System.out.println(">>> ELIXIR-EXPRESS forwarded payment to SORBNET with key: " + key);
    }
}