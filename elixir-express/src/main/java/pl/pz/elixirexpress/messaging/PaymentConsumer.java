package pl.pz.elixirexpress.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    private final SorbnetProducer sorbnetProducer;

    public PaymentConsumer(SorbnetProducer sorbnetProducer) {
        this.sorbnetProducer = sorbnetProducer;
    }

    @KafkaListener(topics = "payments.elixir-express", groupId = "elixir-express-group")
    public void consume(String message) {
        System.out.println(">>> ELIXIR-EXPRESS received payment: " + message);
        sorbnetProducer.sendToSorbnet(message);
    }
}