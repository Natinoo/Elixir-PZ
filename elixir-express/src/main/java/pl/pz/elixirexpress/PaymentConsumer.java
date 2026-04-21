package pl.pz.elixirexpress;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    @KafkaListener(topics = "payments.elixir-express", groupId = "elixir-express-group")
    public void consume(String message) {
        System.out.println(">>> ELIXIR-EXPRESS received payment: " + message);
    }
}