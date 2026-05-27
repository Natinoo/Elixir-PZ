package pl.pz.elixirexpress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);

    @KafkaListener(topics = "payments.elixir-express", groupId = "elixir-express-group")
    public void consume(String message) {
        log.info("ELIXIR-EXPRESS received payment: {}", message);
    }
}