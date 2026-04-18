package pl.pz.elixir;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public MessageController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    public Map<String, String> send(@RequestParam String value) throws ExecutionException, InterruptedException {
        RecordMetadata metadata = kafkaTemplate
                .send("payments", value)
                .get()
                .getRecordMetadata();

        System.out.println("ELIXIR sent to Kafka: topic=" + metadata.topic()
                + ", partition=" + metadata.partition()
                + ", offset=" + metadata.offset()
                + ", value=" + value);

        return Map.of(
                "topic", metadata.topic(),
                "sent", value
        );
    }
}