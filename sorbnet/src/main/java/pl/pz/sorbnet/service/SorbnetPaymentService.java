package pl.pz.sorbnet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.pz.sorbnet.dto.SorbnetPaymentDto;
import pl.pz.sorbnet.model.*;
import pl.pz.sorbnet.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class SorbnetPaymentService {

    private static final Logger log = LoggerFactory.getLogger(SorbnetPaymentService.class);
    private static final String SERVICE = "SORBNET";

    @Value("${sorbnet.overlimit.block-after-hours:2}")
    private int blockAfterHours;

    @Value("${sorbnet.overlimit.block-after-minutes:0}")
    private long blockAfterMinutes;

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

        if (dto.getAmount() == null || dto.getAmount().signum() <= 0) {
            return reject(dto, "INVALID_AMOUNT");
        }

        dto.setSenderBankId(normalizeBankId(dto.getSenderBankId()));
        dto.setReceiverBankId(normalizeBankId(dto.getReceiverBankId()));

        if (paymentRepo.existsById(dto.getPaymentId())) {
            Payment existing = paymentRepo.findById(dto.getPaymentId()).get();
            return Map.of(
                "paymentId", existing.getPaymentId(),
                "status", existing.getStatus().toString(),
                "message", "Przelew już przetworzony (idempotent)"
            );
        }

        BankAccount sender = accountRepo.findByServiceCodeAndBankId(SERVICE, dto.getSenderBankId())
                .orElseThrow(() -> new RuntimeException("Nieznany bank: " + dto.getSenderBankId()));
        BankAccount receiver = accountRepo.findByServiceCodeAndBankId(SERVICE, dto.getReceiverBankId())
                .orElseThrow(() -> new RuntimeException("Nieznany bank: " + dto.getReceiverBankId()));

        // Każdy bank ma w SORBNET dokładnie 1 rachunek — jeśli komunikat nie
        // niesie IBAN-ów, rozstrzygamy po bankId; jeśli niesie cudze, odrzucamy.
        String senderAcc = isBlank(dto.getSenderAccount()) ? sender.getAccountNumber() : dto.getSenderAccount();
        String receiverAcc = isBlank(dto.getReceiverAccount()) ? receiver.getAccountNumber() : dto.getReceiverAccount();
        dto.setSenderAccount(senderAcc);
        dto.setReceiverAccount(receiverAcc);

        if (!senderAcc.equals(sender.getAccountNumber())) {
            return reject(dto, "SENDER_ACCOUNT_MISMATCH");
        }
        if (!receiverAcc.equals(receiver.getAccountNumber())) {
            return reject(dto, "RECEIVER_ACCOUNT_MISMATCH");
        }

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

        ws.convertAndSend("/topic/payments", Map.of(
            "paymentId", payment.getPaymentId(),
            "status", "SETTLED",
            "senderBankId", payment.getSenderBankId(),
            "receiverBankId", payment.getReceiverBankId(),
            "amount", payment.getAmount(),
            "currency", payment.getCurrency(),
            "title", payment.getTitle(),
            "settledAt", payment.getSettledAt().toString()
        ));

        checkDebtAlert(sender);

        return Map.of(
            "paymentId", payment.getPaymentId(),
            "status", "SETTLED",
            "message", "Payment processed",
            "senderBankId", payment.getSenderBankId(),
            "receiverBankId", payment.getReceiverBankId(),
            "senderAccount", payment.getSenderAccount(),
            "receiverAccount", payment.getReceiverAccount(),
            "amount", payment.getAmount(),
            "settledAt", payment.getSettledAt().toString()
        );
    }

    public Map<String, Object> simulateDeposit(String targetBankId, BigDecimal amount, String sourceBankId) {
        String source = normalizeBankId(sourceBankId != null ? sourceBankId : "NBP");

        BankAccount target = accountRepo.findByServiceCodeAndBankId(SERVICE, normalizeBankId(targetBankId))
            .orElseThrow(() -> new RuntimeException("Nieznany bank: " + targetBankId));

        // rachunek źródłowy — jeśli źródło istnieje w SORBNET, bierzemy jego konto
        String sourceAccount = accountRepo.findByServiceCodeAndBankId(SERVICE, source)
                .map(BankAccount::getAccountNumber)
                .orElse(source); // fallback: identyfikator źródła zamiast numeru

        BigDecimal before = target.getBalance();
        target.setBalance(before.add(amount));

        if (target.getOverlimitSince() != null &&
            target.getBalance().compareTo(target.getDebtLimit().negate()) >= 0) {
            target.setOverlimitSince(null);
        }

        accountRepo.save(target);

        Payment p = new Payment();
        p.setPaymentId(UUID.randomUUID().toString());
        p.setSenderBankId(source);
        p.setReceiverBankId(target.getBankId());
        p.setSenderAccount(sourceAccount);                 // not-null w nowym modelu
        p.setReceiverAccount(target.getAccountNumber());   // not-null w nowym modelu
        p.setAmount(amount);
        p.setCurrency("PLN");
        p.setTitle("Symulacja wpłaty kapitału");
        p.setSourceService(SERVICE);
        p.setStatus(PaymentStatus.SETTLED);
        p.setCreatedAt(LocalDateTime.now());
        p.setSettledAt(LocalDateTime.now());
        paymentRepo.save(p);

        notify("notifications.banks", target.getBankId(), Map.of(
            "type", "DEPOSIT_RECEIVED",
            "bankId", target.getBankId(),
            "amount", amount,
            "newBalance", target.getBalance(),
            "sourceBankId", source
        ));

        ws.convertAndSend("/topic/payments", Map.of(
            "paymentId", p.getPaymentId(),
            "status", "SETTLED",
            "senderBankId", p.getSenderBankId(),
            "receiverBankId", p.getReceiverBankId(),
            "amount", p.getAmount(),
            "currency", p.getCurrency(),
            "title", p.getTitle(),
            "settledAt", p.getSettledAt().toString()
        ));

        return Map.of(
            "bankId", target.getBankId(),
            "depositedAmount", amount,
            "balanceBefore", before,
            "balanceAfter", target.getBalance(),
            "overlimitCleared", target.getOverlimitSince() == null
        );
    }

    public Map<String, Object> getAccountStatus(String bankId) {
    BankAccount bank = accountRepo.findByServiceCodeAndBankId(SERVICE, normalizeBankId(bankId))
        .orElseThrow(() -> new RuntimeException("Nieznany bank: " + bankId));

    BigDecimal available = bank.getBalance().add(bank.getDebtLimit());

    // Suma przelewów banku wstrzymanych w gridlocku (nie zaksięgowanych).
    // To one czekają na płynność — od nich liczymy brakującą kwotę.
    BigDecimal heldOutgoing = paymentRepo
            .findBySenderBankIdAndStatus(bank.getBankId(), PaymentStatus.GRIDLOCK_HELD)
            .stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Ile trzeba dopłacić, żeby wstrzymane przelewy zmieściły się w limicie:
    // potrzeba (held - dostępne); jeśli dostępne >= held, nic nie brakuje.
    BigDecimal minDeposit = BigDecimal.ZERO;
    if (heldOutgoing.signum() > 0) {
        BigDecimal shortfall = heldOutgoing.subtract(available);
        if (shortfall.signum() > 0) {
            minDeposit = shortfall;
        }
    } else if (bank.getBalance().compareTo(bank.getDebtLimit().negate()) < 0) {
        // awaryjnie: realne zejście salda poniżej -limit (np. po nettingu z ELIXIR-a)
        minDeposit = bank.getBalance().negate().subtract(bank.getDebtLimit());
    }

   boolean overLimit = bank.getOverlimitSince() != null;

        String blockedBy = null;
        if (overLimit) {
            blockedBy = (blockAfterMinutes > 0
                    ? bank.getOverlimitSince().plusMinutes(blockAfterMinutes)
                    : bank.getOverlimitSince().plusHours(blockAfterHours)
            ).toString();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bankId", bank.getBankId());
        result.put("bankName", bank.getBankName());
        result.put("accountNumber", bank.getAccountNumber());
        result.put("balance", bank.getBalance());
        result.put("debtLimit", bank.getDebtLimit());
        result.put("availableCredit", available);
        result.put("heldAmount", heldOutgoing);
        result.put("minDepositToRestore", minDeposit);
        result.put("blocked", bank.isBlocked());
        result.put("overlimit", overLimit);
        result.put("overlimitSince", overLimit ? bank.getOverlimitSince().toString() : null);
        result.put("blockedIfNotResolvedBy", blockedBy);
        return result;
}

    // ===== prywatne =====

    /** true gdy null lub same białe znaki. */
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private Map<String, Object> holdGridlock(SorbnetPaymentDto dto, BankAccount sender) {
    // przelew BY przekroczył limit -> NIE księgujemy (saldo nietknięte),
    // ale wstrzymanie uruchamia stan alarmowy i licznik do automatycznej blokady.
    // Bank ma czas (block-after-minutes/hours) na uzupełnienie środków skądkolwiek,
    // inaczej autoBlock go zablokuje.
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
        "amount", payment.getAmount(),
        "timestamp", LocalDateTime.now().toString()
    ));

    ws.convertAndSend("/topic/alerts/" + sender.getBankId(), Map.of(
        "alert", true,
        "type", "DEBT_LIMIT_EXCEEDED",
        "message", "Przelew wstrzymany — przekroczyłby limit zadłużenia. "
                + "Uzupełnij środki, aby go rozliczyć i uniknąć blokady.",
        "balance", sender.getBalance(),
        "debtLimit", sender.getDebtLimit(),
        "amount", payment.getAmount(),
        "overlimitSince", sender.getOverlimitSince().toString()
    ));

    return Map.of(
        "paymentId", payment.getPaymentId(),
        "status", "GRIDLOCK_HELD",
        "message", "Payment held in gridlock queue",
        "senderBankId", payment.getSenderBankId(),
        "receiverBankId", payment.getReceiverBankId(),
        "senderAccount", payment.getSenderAccount(),
        "receiverAccount", payment.getReceiverAccount(),
        "amount", payment.getAmount()
    );
}

    private Map<String, Object> reject(SorbnetPaymentDto dto, String reason) {
        Payment payment = buildPayment(dto, PaymentStatus.REJECTED);
        payment.setRejectionReason(reason);
        paymentRepo.save(payment);
        return Map.of(
            "paymentId", payment.getPaymentId(),
            "status", "REJECTED",
            "message", reason,
            "senderBankId", payment.getSenderBankId(),
            "receiverBankId", payment.getReceiverBankId(),
            "senderAccount", payment.getSenderAccount(),
            "receiverAccount", payment.getReceiverAccount(),
            "amount", payment.getAmount()
        );
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
        p.setSenderName(dto.getSenderName());
        p.setReceiverName(dto.getReceiverName());
        p.setTitle(dto.getTitle() != null ? dto.getTitle() : "Przelew SORBNET");
        p.setSourceService(dto.getType() == null || dto.getType().isBlank() ? SERVICE : dto.getType());
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    private String normalizeBankId(String bankId) {
        return bankId == null ? null : bankId.trim().toUpperCase();
    }

    private void notify(String topic, String key, Map<String, Object> payload) {
        try { kafka.send(topic, key, mapper.writeValueAsString(payload)); }
        catch (Exception e) { log.error("Kafka error: {}", e.getMessage()); }
    }
    
}