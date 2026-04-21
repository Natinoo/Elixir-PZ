package pl.pz.elixir.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.PaymentDto;
import pl.pz.elixir.model.PaymentMethod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> processPayment(PaymentDto paymentDto) {
        validate(paymentDto);

        if (paymentDto.getPaymentId() == null || paymentDto.getPaymentId().isBlank()) {
            paymentDto.setPaymentId(UUID.randomUUID().toString());
        }

        PaymentMethod method = resolveMethod(paymentDto);
        String topic = resolveTopic(method);
        String payload = toJson(paymentDto);

        kafkaTemplate.send(topic, paymentDto.getPaymentId(), payload);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentId", paymentDto.getPaymentId());
        response.put("status", "RECEIVED");
        response.put("selectedMethod", method.name());
        response.put("topic", topic);
        return response;
    }

    private PaymentMethod resolveMethod(PaymentDto paymentDto) {
        if (paymentDto.getAmount() != null && paymentDto.getAmount() >= 100000) {
            return PaymentMethod.SORBNET;
        }
        if (Boolean.TRUE.equals(paymentDto.getUrgent())) {
            return PaymentMethod.ELIXIR_EXPRESS;
        }
        return PaymentMethod.ELIXIR;
    }

    private String resolveTopic(PaymentMethod method) {
        return switch (method) {
            case ELIXIR -> "payments.elixir";
            case ELIXIR_EXPRESS -> "payments.elixir-express";
            case SORBNET -> "payments.sorbnet";
        };
    }

    private String toJson(PaymentDto paymentDto) {
        try {
            return objectMapper.writeValueAsString(paymentDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize payment", e);
        }
    }

    private void validate(PaymentDto paymentDto) {
        if (paymentDto == null) {
            throw new IllegalArgumentException("Payment body cannot be null");
        }
        if (paymentDto.getAmount() == null || paymentDto.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (paymentDto.getCurrency() == null || paymentDto.getCurrency().isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (paymentDto.getSenderAccount() == null || paymentDto.getSenderAccount().isBlank()) {
            throw new IllegalArgumentException("Sender account is required");
        }
        if (paymentDto.getReceiverAccount() == null || paymentDto.getReceiverAccount().isBlank()) {
            throw new IllegalArgumentException("Receiver account is required");
        }
    }
}