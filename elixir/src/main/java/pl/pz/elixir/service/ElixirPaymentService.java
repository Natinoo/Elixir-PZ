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
import java.util.Set;
import java.util.UUID;

@Service
public class ElixirPaymentService {

    private static final Set<String> ALLOWED_BANKS = Set.of("BANK_A", "BANK_B", "BANK_C");
    private static final Set<String> ALLOWED_CURRENCIES = Set.of("PLN");

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
        validatePayment(paymentDto);

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

        kafkaTemplate.send("payments.elixir", paymentDto.getPaymentId(), payload);

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

    private void validatePayment(ElixirPaymentDto paymentDto) {
        if (paymentDto == null) {
            throw new IllegalArgumentException("Brak danych przelewu.");
        }

        if (isBlank(paymentDto.getSenderAccount())) {
            throw new IllegalArgumentException("Bank nadawcy jest wymagany.");
        }

        if (isBlank(paymentDto.getReceiverAccount())) {
            throw new IllegalArgumentException("Bank odbiorcy jest wymagany.");
        }

        if (!ALLOWED_BANKS.contains(paymentDto.getSenderAccount())) {
            throw new IllegalArgumentException("Nieprawidłowy bank nadawcy.");
        }

        if (!ALLOWED_BANKS.contains(paymentDto.getReceiverAccount())) {
            throw new IllegalArgumentException("Nieprawidłowy bank odbiorcy.");
        }

        if (paymentDto.getSenderAccount().equals(paymentDto.getReceiverAccount())) {
            throw new IllegalArgumentException("Bank nadawcy i odbiorcy nie mogą być takie same.");
        }

        if (paymentDto.getAmount() == null || paymentDto.getAmount() <= 0) {
            throw new IllegalArgumentException("Kwota musi być większa od zera.");
        }

        if (isBlank(paymentDto.getCurrency())) {
            throw new IllegalArgumentException("Waluta jest wymagana.");
        }

        if (!ALLOWED_CURRENCIES.contains(paymentDto.getCurrency())) {
            throw new IllegalArgumentException("Nieobsługiwana waluta.");
        }

        if (isBlank(paymentDto.getTitle())) {
            throw new IllegalArgumentException("Tytuł przelewu jest wymagany.");
        }

        if (paymentDto.getTitle().length() > 140) {
            throw new IllegalArgumentException("Tytuł przelewu jest za długi.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String toXml(ElixirPaymentDto paymentDto) {
        try {
            return xmlMapper.writeValueAsString(paymentDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize payment to XML", e);
        }
    }
}