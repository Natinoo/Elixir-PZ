package pl.pz.elixir.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ElixirPaymentService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final XmlMapper xmlMapper;
    private final PaymentRepository paymentRepository;

    public ElixirPaymentService(
            KafkaTemplate<String, String> kafkaTemplate,
            XmlMapper xmlMapper,
            PaymentRepository paymentRepository
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.xmlMapper = xmlMapper;
        this.paymentRepository = paymentRepository;
    }

    public Map<String, Object> processPayment(ElixirPaymentDto paymentDto) {
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
                PaymentStatus.QUEUED,
                LocalDateTime.now()
        );

        paymentRepository.save(payment);

        String payload = toXml(paymentDto);

        kafkaTemplate.send(
                "payments.elixir",
                paymentDto.getPaymentId(),
                payload
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentId", paymentDto.getPaymentId());
        response.put("status", "QUEUED_FOR_SESSION");

        return response;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    public void updatePaymentStatus(String paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    private String toXml(ElixirPaymentDto paymentDto) {
        try {
            return xmlMapper.writeValueAsString(paymentDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize payment to XML", e);
        }
    }
}