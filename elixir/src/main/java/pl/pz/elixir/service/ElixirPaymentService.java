package pl.pz.elixir.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.ElixirPaymentDto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ElixirPaymentService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ElixirPaymentService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> processPayment(ElixirPaymentDto paymentDto) {

        // generowanie ID
        if (paymentDto.getPaymentId() == null || paymentDto.getPaymentId().isBlank()) {
            paymentDto.setPaymentId(UUID.randomUUID().toString());
        }

        String payload = toJson(paymentDto);

        kafkaTemplate.send("payments.elixir", paymentDto.getPaymentId(), payload);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentId", paymentDto.getPaymentId());
        response.put("status", "QUEUED_FOR_SESSION");

        return response;
    }

    private String toJson(ElixirPaymentDto paymentDto) {
        try {
            return objectMapper.writeValueAsString(paymentDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize payment", e);
        }
    }
}