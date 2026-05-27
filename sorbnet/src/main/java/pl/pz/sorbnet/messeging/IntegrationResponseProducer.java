package pl.pz.sorbnet.messeging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class IntegrationResponseProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public IntegrationResponseProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendToElixir(String key, String payload) {
        kafkaTemplate.send("responses.elixir", key, payload);
    }

    public void sendToExpress(String key, String payload) {
        kafkaTemplate.send("responses.elixir-express", key, payload);
    }
}