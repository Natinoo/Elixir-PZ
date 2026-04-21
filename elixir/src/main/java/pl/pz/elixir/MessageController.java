package pl.pz.elixir;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public MessageController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    public Map<String, String> send(@RequestParam String topic, @RequestParam String value) {
        kafkaTemplate.send(topic, value);

        return Map.of(
                "topic", topic,
                "sent", value
        );
    }
}