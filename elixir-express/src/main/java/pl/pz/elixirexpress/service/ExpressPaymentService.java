package pl.pz.elixirexpress.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pl.pz.elixirexpress.dto.ExpressPaymentDto;
import pl.pz.elixirexpress.model.Payment;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.repository.PaymentRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpressPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ExpressPaymentService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final XmlMapper xmlMapper;
    private final PaymentRepository paymentRepository;
    private volatile boolean gridlockActive = false;

    public ExpressPaymentService(KafkaTemplate<String, String> kafkaTemplate,
                                 XmlMapper xmlMapper,
                                 PaymentRepository paymentRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.xmlMapper = xmlMapper;
        this.paymentRepository = paymentRepository;
    }

    public Map<String, Object> processPayment(ExpressPaymentDto paymentDto) {
        validate(paymentDto);

        if (gridlockActive) {
            throw new IllegalArgumentException("System temporarily unavailable due to gridlock. Try again later.");
        }

        if (paymentDto.getPaymentId() == null || paymentDto.getPaymentId().isBlank()) {
            paymentDto.setPaymentId(UUID.randomUUID().toString());
        }

        Payment payment = new Payment(
                paymentDto.getSenderAccount(),
                paymentDto.getReceiverAccount(),
                paymentDto.getAmount(),
                paymentDto.getCurrency(),
                paymentDto.getTitle()
        );
        payment.setPaymentId(paymentDto.getPaymentId());
        payment.setStatus(PaymentStatus.QUEUED);
        paymentRepository.save(payment);

        String payload = toXml(paymentDto);
        kafkaTemplate.send("payments.sorbnet", paymentDto.getPaymentId(), payload);

        return Map.of(
                "paymentId", paymentDto.getPaymentId(),
                "status", PaymentStatus.QUEUED.name(),
                "channel", "EXPRESS"
        );
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId).orElse(null);
    }

    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    public boolean cancelPayment(String paymentId) {
        Payment payment = getPaymentById(paymentId);
        if (payment == null || payment.getStatus() == PaymentStatus.PROCESSED) {
            return false;
        }
        payment.setStatus(PaymentStatus.REJECTED);
        paymentRepository.save(payment);
        return true;
    }

    public void updatePaymentStatus(String paymentId, PaymentStatus status) {
        paymentRepository.findById(paymentId).ifPresent(p -> {
            p.setStatus(status);
            paymentRepository.save(p);
            log.info("Updated payment {} to status {}", paymentId, status);
        });
    }

    // Obsługa gridlock – nasłuch na topic events.gridlock
    @KafkaListener(topics = "events.gridlock", groupId = "elixir-express-group")
    public void handleGridlock(String message) {
        log.warn("Gridlock event received: {}. Blocking new payments.", message);
        gridlockActive = true;
        // W rzeczywistym systemie można uruchomić timer do automatycznego odblokowania
    }

    @KafkaListener(topics = "events.emergency", groupId = "elixir-express-group")
    public void handleEmergency(String message) {
        log.warn("EMERGENCY event received: {}. System in emergency mode – blocking new payments for 5 minutes.", message);
        gridlockActive = true;
        // Automatyczne odblokowanie po 5 minutach
        new Thread(() -> {
            try {
                Thread.sleep(300000);
                gridlockActive = false;
                log.info("Emergency mode lifted. Payments accepted again.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
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