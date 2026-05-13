package pl.pz.sorbnet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pz.sorbnet.dto.SorbnetPaymentDto;
import pl.pz.sorbnet.model.*;
import pl.pz.sorbnet.repository.*;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class SorbnetPaymentService {

    private static final Logger log = LoggerFactory.getLogger(SorbnetPaymentService.class);

    private final BankAccountRepository accountRepo;
    private final PaymentRepository paymentRepo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    private final SimpMessagingTemplate ws;

    public SorbnetPaymentService(BankAccountRepository accountRepo,
                              PaymentRepository paymentRepo,
                              KafkaTemplate<String, String> kafka,
                              ObjectMapper mapper,
                              SimpMessagingTemplate ws) {
    this.accountRepo = accountRepo;
    this.paymentRepo = paymentRepo;
    this.kafka = kafka;
    this.mapper = mapper;
    this.ws = ws;
    }

    public Map<String, Object> process(SorbnetPaymentDto dto) {
        if (dto.getPaymentId() == null) dto.setPaymentId(UUID.randomUUID().toString());

        if (paymentRepo.existsById(dto.getPaymentId())) {
        Payment existing = paymentRepo.findById(dto.getPaymentId()).get();
        return Map.of(
            "paymentId", existing.getPaymentId(),
            "status", existing.getStatus().toString(),
            "info", "Przelew już przetworzony (idempotent)"
        );
        }
        BankAccount sender = accountRepo.findById(dto.getSenderBankId())
                .orElseThrow(() -> new RuntimeException("Nieznany bank: " + dto.getSenderBankId()));
        BankAccount receiver = accountRepo.findById(dto.getReceiverBankId())
                .orElseThrow(() -> new RuntimeException("Nieznany bank: " + dto.getReceiverBankId()));

        if (sender.isBlocked())   return reject(dto, "SENDER_BLOCKED");
        if (receiver.isBlocked()) return reject(dto, "RECEIVER_BLOCKED");

        BigDecimal newBalance = sender.getBalance().subtract(dto.getAmount());

        if (newBalance.compareTo(sender.getDebtLimit().negate()) < 0) {
            return holdGridlock(dto, sender);
        }

        sender.setBalance(newBalance);
        receiver.setBalance(receiver.getBalance().add(dto.getAmount()));

        if (sender.getOverlimitSince() != null &&
            newBalance.compareTo(sender.getDebtLimit().negate()) >= 0) {
            sender.setOverlimitSince(null);
        }

        accountRepo.saveAll(List.of(sender, receiver));

        Payment payment = buildPayment(dto, PaymentStatus.SETTLED);
        payment.setSettledAt(LocalDateTime.now());
        paymentRepo.save(payment);

        String xml = toXml(dto);
        kafka.send("notifications.banks", payment.getReceiverBankId(), xml);
        kafka.send("notifications.banks", payment.getSenderBankId(), xml);

        checkDebtAlert(sender);

        return Map.of(
            "paymentId", payment.getPaymentId(),
            "status", "SETTLED",
            "settledAt", payment.getSettledAt().toString()
        );
    }

    public Map<String, Object> simulateDeposit(String targetBankId, BigDecimal amount, String sourceBankId) {
        BankAccount target = accountRepo.findById(targetBankId)
            .orElseThrow(() -> new RuntimeException("Nieznany bank: " + targetBankId));

        BigDecimal before = target.getBalance();
        target.setBalance(before.add(amount));

        if (target.getOverlimitSince() != null &&
            target.getBalance().compareTo(target.getDebtLimit().negate()) >= 0) {
            target.setOverlimitSince(null);
        }

        accountRepo.save(target);

        Payment p = new Payment();
        p.setPaymentId(UUID.randomUUID().toString());
        p.setSenderBankId(sourceBankId != null ? sourceBankId : "NBP");
        p.setReceiverBankId(targetBankId);
        p.setAmount(amount);
        p.setCurrency("PLN");
        p.setTitle("Symulacja wpłaty kapitału");
        p.setStatus(PaymentStatus.SETTLED);
        p.setCreatedAt(LocalDateTime.now());
        p.setSettledAt(LocalDateTime.now());
        paymentRepo.save(p);

        notify("notifications.banks", targetBankId, Map.of(
            "type", "DEPOSIT_RECEIVED",
            "bankId", targetBankId,
            "amount", amount,
            "newBalance", target.getBalance(),
            "sourceBankId", sourceBankId != null ? sourceBankId : "NBP"
        ));

        return Map.of(
            "bankId", targetBankId,
            "depositedAmount", amount,
            "balanceBefore", before,
            "balanceAfter", target.getBalance(),
            "overlimitCleared", target.getOverlimitSince() == null
        );
    }

    public Map<String, Object> getAccountStatus(String bankId) {
        BankAccount bank = accountRepo.findById(bankId)
            .orElseThrow(() -> new RuntimeException("Nieznany bank: " + bankId));
    
        BigDecimal available = bank.getBalance().add(bank.getDebtLimit());
        BigDecimal minDeposit = BigDecimal.ZERO;
    
        if (bank.getBalance().compareTo(bank.getDebtLimit().negate()) < 0) {
            minDeposit = bank.getBalance().negate().subtract(bank.getDebtLimit());
        }
    
        Map<String, Object> result = new HashMap<>();
        result.put("bankId", bank.getBankId());
        result.put("bankName", bank.getBankName());
        result.put("balance", bank.getBalance());
        result.put("debtLimit", bank.getDebtLimit());
        result.put("availableCredit", available);
        result.put("minDepositToRestore", minDeposit);
        result.put("blocked", bank.isBlocked());
        result.put("overlimitSince", bank.getOverlimitSince() != null
                ? bank.getOverlimitSince().toString() : null);
        return result;
    }

    private Map<String, Object> holdGridlock(SorbnetPaymentDto dto, BankAccount sender) {
        if (sender.getOverlimitSince() == null) {
            sender.setOverlimitSince(LocalDateTime.now());
            accountRepo.save(sender);
        }

        Payment payment = buildPayment(dto, PaymentStatus.GRIDLOCK_HELD);
        paymentRepo.save(payment);

        notify("events.gridlock", sender.getBankId(), Map.of(
            "bankId", sender.getBankId(),
            "paymentId", payment.getPaymentId(),
            "balance", sender.getBalance(),
            "debtLimit", sender.getDebtLimit(),
            "timestamp", LocalDateTime.now().toString()
        ));
        notify("events.emergency", sender.getBankId(), Map.of(
            "type", "DEBT_LIMIT_EXCEEDED",
            "bankId", sender.getBankId(),
            "balance", sender.getBalance(),
            "debtLimit", sender.getDebtLimit()
        ));

        // WebSocket push do GUI banku 
    ws.convertAndSend("/topic/alerts/" + sender.getBankId(), Map.of(
        "alert", true,
        "type", "DEBT_LIMIT_EXCEEDED",
        "message", "Przekroczono limit zadłużenia. Należy niezwłocznie uzupełnić środki.",
        "balance", sender.getBalance(),
        "debtLimit", sender.getDebtLimit(),
        "overlimitSince", sender.getOverlimitSince().toString(),
        "blockedIfNotResolvedBy", sender.getOverlimitSince().plusHours(2).toString()
    ));


        return Map.of("paymentId", payment.getPaymentId(), "status", "GRIDLOCK_HELD");
    }

    private Map<String, Object> reject(SorbnetPaymentDto dto, String reason) {
        Payment payment = buildPayment(dto, PaymentStatus.REJECTED);
        payment.setRejectionReason(reason);
        paymentRepo.save(payment);
        return Map.of("paymentId", payment.getPaymentId(), "status", "REJECTED", "reason", reason);
    }

    private void checkDebtAlert(BankAccount bank) {
        BigDecimal threshold = bank.getDebtLimit().multiply(BigDecimal.valueOf(0.8));
        if (bank.getBalance().compareTo(threshold.negate()) < 0) {
            notify("events.emergency", bank.getBankId(), Map.of(
                "type", "APPROACHING_DEBT_LIMIT",
                "bankId", bank.getBankId(),
                "balance", bank.getBalance(),
                "debtLimit", bank.getDebtLimit()
            ));
        // WebSocket push
        ws.convertAndSend("/topic/alerts/" + bank.getBankId(), Map.of(
            "alert", true,
            "type", "APPROACHING_DEBT_LIMIT",
            "message", "Saldo zbliża się do limitu zadłużenia.",
            "balance", bank.getBalance(),
            "debtLimit", bank.getDebtLimit()
        ));   
        }
    }

    private Payment buildPayment(SorbnetPaymentDto dto, PaymentStatus status) {
        Payment p = new Payment();
        p.setPaymentId(dto.getPaymentId());
        p.setSenderBankId(dto.getSenderBankId());
        p.setReceiverBankId(dto.getReceiverBankId());
        p.setAmount(dto.getAmount());
        p.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "PLN");
        p.setSenderAccount(dto.getSenderAccount());
        p.setReceiverAccount(dto.getReceiverAccount());
        p.setTitle(dto.getTitle());
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    private String toXml(SorbnetPaymentDto dto) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(SorbnetPaymentDto.class);
            Marshaller m = ctx.createMarshaller();
            StringWriter sw = new StringWriter();
            m.marshal(dto, sw);
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("XML marshal error", e);
        }
    }

    private void notify(String topic, String key, Map<String, Object> payload) {
        try { kafka.send(topic, key, mapper.writeValueAsString(payload)); }
        catch (Exception e) { log.error("Kafka error: {}", e.getMessage()); }
    }
}