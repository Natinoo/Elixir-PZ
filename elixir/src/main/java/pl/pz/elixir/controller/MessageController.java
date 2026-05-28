package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "Narzędzie diagnostyczne – wysyłanie dowolnych wiadomości Kafka", description = "Endpoint przeznaczony dla administratorów do ręcznego wysyłania komunikatów na topiki Kafka.")
public class MessageController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public MessageController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Wyślij wiadomość na wskazany topic", description = "Umożliwia ręczne opublikowanie wiadomości (tekstowej) na dowolnym topiku Kafka.")
    @ApiResponse(responseCode = "200", description = "Wiadomość wysłana",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"topic\":\"payments.sorbnet\",\"sent\":\"{\\\"paymentId\\\":\\\"123\\\"}\"}")))
    public Map<String, String> send(
            @Parameter(description = "Nazwa topicu Kafka", required = true, example = "payments.sorbnet")
            @RequestParam String topic,
            @Parameter(description = "Treść wiadomości (JSON lub XML)", required = true, example = "{\"paymentId\":\"123\"}")
            @RequestParam String value) {
        kafkaTemplate.send(topic, value);
        return Map.of("topic", topic, "sent", value);
    }
}