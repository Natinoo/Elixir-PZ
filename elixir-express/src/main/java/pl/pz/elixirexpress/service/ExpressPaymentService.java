package pl.pz.elixirexpress.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pl.pz.elixirexpress.dto.ExpressPaymentDto;
import pl.pz.elixirexpress.model.Payment;
import pl.pz.elixirexpress.model.PaymentStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpressPaymentService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final XmlMapper xmlMapper;
    private final List<Payment> payments = new ArrayList<>();

    public ExpressPaymentService(KafkaTemplate<String, String> kafkaTemplate, XmlMapper xmlMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.xmlMapper = xmlMapper;
    }

    public Map<String, Object> processPayment(ExpressPaymentDto paymentDto) {
        validate(paymentDto);

        if (paymentDto.getPaymentId() == null || paymentDto.getPaymentId().isBlank()) {
            paymentDto.setPaymentId(UUID.randomUUID().toString());
        }

        Payment payment = new Payment(
                paymentDto.getPaymentId(),
                paymentDto.getSenderAccount(),
                paymentDto.getReceiverAccount(),
                paymentDto.getAmount(),
                paymentDto.getCurrency(),
                paymentDto.getTitle(),
                PaymentStatus.ACCEPTED
        );

        payments.add(payment);

        String payload = toXml(paymentDto);

        kafkaTemplate.send("payments.sorbnet", paymentDto.getPaymentId(), payload);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentId", paymentDto.getPaymentId());
        response.put("status", PaymentStatus.ACCEPTED.name());
        response.put("channel", "EXPRESS");
        return response;
    }

    public List<Payment> getAllPayments() {
        return payments;
    }

    public Payment getPaymentById(String paymentId) {
        return payments.stream()
                .filter(payment -> payment.getPaymentId().equals(paymentId))
                .findFirst()
                .orElse(null);
    }

    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        List<Payment> result = new ArrayList<>();
        for (Payment payment : payments) {
            if (payment.getStatus() == status) {
                result.add(payment);
            }
        }
        return result;
    }

    public boolean cancelPayment(String paymentId) {
        Payment payment = getPaymentById(paymentId);
        if (payment == null) {
            return false;
        }

        if (payment.getStatus() == PaymentStatus.PROCESSED || payment.getStatus() == PaymentStatus.SENT) {
            return false;
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        return true;
    }

    public void updatePaymentStatus(String paymentId, PaymentStatus status) {
        Payment payment = getPaymentById(paymentId);
        if (payment != null) {
            payment.setStatus(status);
        }
    }

    private String toXml(ExpressPaymentDto paymentDto) {
        try {
            return xmlMapper.writeValueAsString(paymentDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize payment to XML", e);
        }
    }

    private void validate(ExpressPaymentDto paymentDto) {
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
        if (paymentDto.getTitle() == null || paymentDto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
    }
}