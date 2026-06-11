package pl.pz.elixir.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.pz.elixir.service.SessionService;

@Component
public class ElixirKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ElixirKafkaConsumer.class);

    private final SessionService sessionService;

    public ElixirKafkaConsumer(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @KafkaListener(topics = "payments.elixir", groupId = "elixir-group")
    public void consume(String message) {
        log.info("ELIXIR received ISO 20022 payment from Kafka");
        sessionService.addToSession(message);
    }
}