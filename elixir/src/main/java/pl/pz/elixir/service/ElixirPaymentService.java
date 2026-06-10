package pl.pz.elixir.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.repository.PaymentRepository;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ElixirPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ElixirPaymentService.class);
    private static final Set<String> ALLOWED_BANKS = Set.of("BANK_A", "BANK_B", "BANK_C");
    private static final Set<String> ALLOWED_CURRENCIES = Set.of("PLN");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentRepository paymentRepository;
    private final JAXBContext jaxbContext;

    public ElixirPaymentService(
            KafkaTemplate<String, String> kafkaTemplate,
            PaymentRepository paymentRepository
    ) throws Exception {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentRepository = paymentRepository;
        this.jaxbContext = JAXBContext.newInstance(ElixirPaymentDto.class);
        log.info("ElixirPaymentService initialized");
    }

    public String processPayment(ElixirPaymentDto paymentDto) {
        log.info("processPayment called: senderAccount={}, receiverAccount={}, amount={}",
                paymentDto.getSenderAccount(), paymentDto.getReceiverAccount(), paymentDto.getAmount());

        // Ustawienie typu przelewu (dla rozróżnienia w bazie i w konsumentach)
        paymentDto.setType("ELIXIR");

        // Uzupełnienie pól bankId na podstawie konta (jeśli brak)
        if (paymentDto.getSenderBankId() == null || paymentDto.getSenderBankId().isBlank()) {
            paymentDto.setSenderBankId(paymentDto.getSenderAccount());
        }
        if (paymentDto.getReceiverBankId() == null || paymentDto.getReceiverBankId().isBlank()) {
            paymentDto.setReceiverBankId(paymentDto.getReceiverAccount());
        }

        validatePayment(paymentDto);
        log.info("Validation passed");

        if (paymentDto.getPaymentId() == null || paymentDto.getPaymentId().isBlank()) {
            paymentDto.setPaymentId(UUID.randomUUID().toString());
            log.info("Generated new paymentId: {}", paymentDto.getPaymentId());
        }

        // Mapowanie numerów kont (dla Sorbnet) – zachowując oryginalne identyfikatory banków
        String originalSenderAccount = paymentDto.getSenderAccount();
        String originalReceiverAccount = paymentDto.getReceiverAccount();
        String mappedSenderAccount = mapBankToAccount(originalSenderAccount);
        String mappedReceiverAccount = mapBankToAccount(originalReceiverAccount);
        if (mappedSenderAccount != null) paymentDto.setSenderAccount(mappedSenderAccount);
        if (mappedReceiverAccount != null) paymentDto.setReceiverAccount(mappedReceiverAccount);

        Payment payment = new Payment(
                paymentDto.getPaymentId(),
                paymentDto.getSenderBankId(),
                paymentDto.getReceiverBankId(),
                paymentDto.getSenderAccount(),
                paymentDto.getReceiverAccount(),
                paymentDto.getAmount(),
                paymentDto.getCurrency(),
                paymentDto.getTitle(),
                PaymentStatus.QUEUED,
                LocalDateTime.now(),
                "ELIXIR"  // typ przelewu
        );
        paymentRepository.save(payment);
        log.info("Payment saved to DB, id={}", payment.getPaymentId());

        String payload = toXml(paymentDto);
        kafkaTemplate.send("payments.elixir", paymentDto.getPaymentId(), payload);
        log.info("Message sent to Kafka topic 'payments.elixir'");

        String responseXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<PaymentResponse>" +
                "<paymentId>" + paymentDto.getPaymentId() + "</paymentId>" +
                "<status>QUEUED_FOR_SESSION</status>" +
                "</PaymentResponse>";
        log.info("Returning response XML: {}", responseXml);
        return responseXml;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    public void updatePaymentStatus(String paymentId, PaymentStatus status) {
        log.info("Updating payment {} to status {}", paymentId, status);
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("Payment not found for status update: {}", paymentId);
            return;
        }
        if (payment.getStatus() == status) {
            log.info("Payment {} already has status {}", paymentId, status);
            return;
        }
        payment.setStatus(status);
        paymentRepository.save(payment);
        log.info("Payment {} updated successfully to status {}", paymentId, status);
    }

    private void validatePayment(ElixirPaymentDto paymentDto) {
        if (paymentDto == null) throw new IllegalArgumentException("Brak danych przelewu.");

        if (isBlank(paymentDto.getSenderBankId())) throw new IllegalArgumentException("Bank nadawcy jest wymagany.");
        if (isBlank(paymentDto.getReceiverBankId())) throw new IllegalArgumentException("Bank odbiorcy jest wymagany.");
        if (!ALLOWED_BANKS.contains(paymentDto.getSenderBankId())) throw new IllegalArgumentException("Nieprawidłowy bank nadawcy.");
        if (!ALLOWED_BANKS.contains(paymentDto.getReceiverBankId())) throw new IllegalArgumentException("Nieprawidłowy bank odbiorcy.");
        if (paymentDto.getSenderBankId().equals(paymentDto.getReceiverBankId())) {
            throw new IllegalArgumentException("Bank nadawcy i odbiorcy nie mogą być takie same.");
        }

        if (isBlank(paymentDto.getSenderAccount())) throw new IllegalArgumentException("Rachunek nadawcy jest wymagany.");
        if (isBlank(paymentDto.getReceiverAccount())) throw new IllegalArgumentException("Rachunek odbiorcy jest wymagany.");
        if (paymentDto.getSenderAccount().equals(paymentDto.getReceiverAccount())) {
            throw new IllegalArgumentException("Rachunek nadawcy i odbiorcy nie mogą być takie same.");
        }

        if (paymentDto.getAmount() == null || paymentDto.getAmount() <= 0) throw new IllegalArgumentException("Kwota musi być większa od zera.");
        if (isBlank(paymentDto.getCurrency())) throw new IllegalArgumentException("Waluta jest wymagana.");
        if (!ALLOWED_CURRENCIES.contains(paymentDto.getCurrency())) throw new IllegalArgumentException("Nieobsługiwana waluta.");
        if (isBlank(paymentDto.getTitle())) throw new IllegalArgumentException("Tytuł przelewu jest wymagany.");
        if (paymentDto.getTitle().length() > 140) throw new IllegalArgumentException("Tytuł przelewu jest za długi.");
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private String toXml(ElixirPaymentDto paymentDto) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            marshaller.marshal(paymentDto, sw);
            return sw.toString();
        } catch (Exception e) {
            log.error("Cannot serialize payment to XML", e);
            throw new RuntimeException("Cannot serialize payment to XML", e);
        }
    }

    private String mapBankToAccount(String bankId) {
        if (bankId == null) return null;
        return switch (bankId) {
            case "BANK_A" -> "11111100000000000000000001";
            case "BANK_B" -> "22222200000000000000000002";
            case "BANK_C" -> "33333300000000000000000003";
            default -> bankId;
        };
    }
}