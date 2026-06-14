package pl.pz.elixirexpress.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pz.elixirexpress.dto.ExpressPaymentDto;
import pl.pz.elixirexpress.model.BankAccount;
import pl.pz.elixirexpress.model.Payment;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.repository.BankAccountRepository;
import pl.pz.elixirexpress.repository.PaymentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ExpressPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ExpressPaymentService.class);
    private static final String SERVICE_CODE = "EXPRESS";
    private static final Set<String> ALLOWED_BANKS = Set.of("BANK_A", "BANK_B", "BANK_C");
    private static final Set<String> ALLOWED_CURRENCIES = Set.of("PLN");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentRepository paymentRepository;
    private final BankAccountRepository bankAccountRepository;

    private volatile boolean gridlockActive = false;

    public ExpressPaymentService(KafkaTemplate<String, String> kafkaTemplate,
                                 PaymentRepository paymentRepository,
                                 BankAccountRepository bankAccountRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentRepository = paymentRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional
    public Map<String, Object> processPayment(ExpressPaymentDto paymentDto) {
        if (paymentDto == null) {
            throw new IllegalArgumentException("Brak danych przelewu.");
        }

        paymentDto.setType(SERVICE_CODE);
        if (isBlank(paymentDto.getPaymentId())) {
            paymentDto.setPaymentId("EXP-" + UUID.randomUUID());
        }

        validate(paymentDto);

        Payment payment = toEntity(paymentDto, PaymentStatus.QUEUED);

        if (gridlockActive) {
            payment.setStatus(PaymentStatus.GRIDLOCK_HELD);
            payment.setHeldReason("System Express jest w trybie gridlock/emergency.");
            paymentRepository.save(payment);
            return response(payment, payment.getHeldReason(), BigDecimal.ZERO);
        }

        BankAccount sender = getBank(paymentDto.getSenderBankId());
        BankAccount receiver = getBank(paymentDto.getReceiverBankId());

        if (sender.isBlocked()) {
            payment.setStatus(PaymentStatus.BLOCKED);
            payment.setHeldReason("Bank nadawcy jest zablokowany.");
            paymentRepository.save(payment);
            return response(payment, payment.getHeldReason(), BigDecimal.ZERO);
        }

        if (receiver.isBlocked()) {
            payment.setStatus(PaymentStatus.BLOCKED);
            payment.setHeldReason("Bank odbiorcy jest zablokowany.");
            paymentRepository.save(payment);
            return response(payment, payment.getHeldReason(), BigDecimal.ZERO);
        }

        BigDecimal requiredTopUp = requiredTopUpForPayment(sender, paymentDto.getAmount());
        if (requiredTopUp.compareTo(BigDecimal.ZERO) > 0) {
            markBankWaitingForLiquidity(sender);

            payment.setStatus(PaymentStatus.GRIDLOCK_HELD);
            payment.setHeldReason("Przelew zatrzymany: bank przekroczyłby limit zadłużenia w EXPRESS. "
                    + "Minimalne wymagane zasilenie do limitu: " + formatAmount(requiredTopUp) + " PLN.");
            paymentRepository.save(payment);

            sendLiquidityRequest(payment, sender, requiredTopUp);

            log.warn("EXPRESS payment held due to liquidity: paymentId={}, bank={}, balance={}, debtLimit={}, amount={}, available={}, requiredTopUp={}",
                    payment.getPaymentId(), sender.getBankId(), sender.getBalance(), sender.getDebtLimit(),
                    payment.getAmount(), availableForDebit(sender), requiredTopUp);

            return response(payment, payment.getHeldReason(), requiredTopUp);
        }

        applyExpressDebitCredit(sender, receiver, paymentDto.getAmount());
        payment.setStatus(PaymentStatus.PROCESSED);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setHeldReason(null);
        paymentRepository.save(payment);

        log.info("EXPRESS payment settled locally: id={}, senderBank={}, receiverBank={}, amount={}, senderBalance={}, receiverBalance={}",
                payment.getPaymentId(), payment.getSenderBankId(), payment.getReceiverBankId(), payment.getAmount(),
                sender.getBalance(), receiver.getBalance());

        return response(payment, "Przelew express rozliczony lokalnie w systemie Express.", BigDecimal.ZERO);
    }

    @Transactional
    public boolean retryHeldPayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.GRIDLOCK_HELD) {
            return false;
        }

        BankAccount sender = getBank(payment.getSenderBankId());
        BankAccount receiver = getBank(payment.getReceiverBankId());

        if (sender.isBlocked() || receiver.isBlocked()) {
            return false;
        }

        if (requiredTopUpForPayment(sender, payment.getAmount()).compareTo(BigDecimal.ZERO) > 0) {
            markBankWaitingForLiquidity(sender);
            return false;
        }

        applyExpressDebitCredit(sender, receiver, payment.getAmount());
        payment.setStatus(PaymentStatus.PROCESSED);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setHeldReason(null);
        paymentRepository.save(payment);

        refreshLiquidityMarkerAfterRetry(sender.getBankId());
        log.info("Held EXPRESS payment retried and settled locally: {}", payment.getPaymentId());
        return true;
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

    public List<BankAccount> getAllAccounts() {
        return bankAccountRepository.findAll();
    }

    public BankAccount getAccount(String bankId) {
        return getBank(bankId);
    }

    @Transactional
    public BankAccount topUpBank(String bankId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota zasilenia musi być większa od zera.");
        }

        BankAccount bank = getBank(bankId);
        BigDecimal requiredTopUp = requiredTopUpForHeldPayments(bank);

        if (requiredTopUp.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(requiredTopUp) > 0) {
            throw new IllegalArgumentException("Zasilenie przekracza minimalną kwotę wymaganą do powrotu do limitu. "
                    + "Maksymalnie wpisz: " + formatAmount(requiredTopUp) + " PLN.");
        }

        bank.setBalance(bank.getBalance().add(amount));

        if (bank.isBlocked() && requiredTopUpForHeldPayments(bank).compareTo(BigDecimal.ZERO) == 0 && !bank.isOverLimit()) {
            bank.setBlocked(false);
            bank.setBlockedAt(null);
            log.info("EXPRESS bank automatically unblocked after top-up: {}", bankId);
        }

        bankAccountRepository.save(bank);
        int retried = retryHeldPaymentsForBank(bankId);
        refreshLiquidityMarkerAfterRetry(bankId);

        BankAccount refreshed = getBank(bankId);
        log.info("EXPRESS bank top-up: bank={}, amount={}, balance={}, retriedHeldPayments={}",
                bankId, amount, refreshed.getBalance(), retried);
        return refreshed;
    }

    @Transactional
    public BankAccount unblockBank(String bankId) {
        BankAccount bank = getBank(bankId);
        bank.setBlocked(false);
        bank.setBlockedAt(null);
        bankAccountRepository.save(bank);

        int retried = retryHeldPaymentsForBank(bankId);
        refreshLiquidityMarkerAfterRetry(bankId);

        log.info("EXPRESS bank unblocked manually: bank={}, retriedHeldPayments={}", bankId, retried);
        return getBank(bankId);
    }

    @Transactional
    public void blockBank(String bankId) {
        BankAccount bank = getBank(bankId);
        if (!bank.isBlocked()) {
            bank.setBlocked(true);
            bank.setBlockedAt(LocalDateTime.now());
            bankAccountRepository.save(bank);
            log.warn("EXPRESS bank blocked: {}", bankId);
        }
    }

    @Transactional
    public int retryHeldPaymentsForBank(String bankId) {
        List<Payment> held = paymentRepository.findBySenderBankIdAndStatus(bankId, PaymentStatus.GRIDLOCK_HELD);
        int retried = 0;
        for (Payment payment : held) {
            if (retryHeldPayment(payment.getPaymentId())) {
                retried++;
            }
        }
        return retried;
    }

    @Transactional
    public boolean cancelPayment(String paymentId) {
        Payment payment = getPaymentById(paymentId);
        if (payment == null || payment.getStatus() == PaymentStatus.PROCESSED) {
            return false;
        }
        payment.setStatus(PaymentStatus.REJECTED);
        paymentRepository.save(payment);
        log.info("Payment cancelled: {}", paymentId);
        return true;
    }

    @Transactional
    public void updatePaymentStatus(String paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("EXPRESS payment not found for status update: {}", paymentId);
            return;
        }
        payment.setStatus(status);
        if (status == PaymentStatus.PROCESSED || status == PaymentStatus.SETTLED) {
            payment.setProcessedAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);
        log.info("Updated EXPRESS payment {} to status {}", paymentId, status);
    }

    @org.springframework.kafka.annotation.KafkaListener(topics = "events.gridlock", groupId = "elixir-express-group")
    public void handleGridlock(String message) {
        log.warn("GRIDLOCK event received: {}. Blocking new payments.", message);
        gridlockActive = true;
    }

    @org.springframework.kafka.annotation.KafkaListener(topics = "events.emergency", groupId = "elixir-express-group")
    public void handleEmergency(String message) {
        log.warn("EMERGENCY event received: {}. System in emergency mode for 5 minutes.", message);
        gridlockActive = true;
        new Thread(() -> {
            try {
                Thread.sleep(300000);
                gridlockActive = false;
                log.info("Emergency mode lifted. EXPRESS payments accepted again.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private Payment toEntity(ExpressPaymentDto dto, PaymentStatus status) {
        return new Payment(
                dto.getPaymentId(),
                dto.getSenderName(),
                dto.getReceiverName(),
                normalizeIban(dto.getSenderAccount()),
                normalizeIban(dto.getReceiverAccount()),
                dto.getAmount(),
                dto.getCurrency(),
                dto.getTitle(),
                dto.getSenderBankId(),
                dto.getReceiverBankId(),
                status,
                SERVICE_CODE
        );
    }

    private void applyExpressDebitCredit(BankAccount sender, BankAccount receiver, BigDecimal amount) {
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        refreshActualOverlimitMarker(sender);
        refreshActualOverlimitMarker(receiver);
        bankAccountRepository.save(sender);
        bankAccountRepository.save(receiver);
    }

    private BigDecimal requiredTopUpForPayment(BankAccount sender, BigDecimal amount) {
        BigDecimal missing = safe(amount).subtract(availableForDebit(sender));
        return missing.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal requiredTopUpForHeldPayments(BankAccount bank) {
        List<Payment> held = paymentRepository.findBySenderBankIdAndStatus(bank.getBankId(), PaymentStatus.GRIDLOCK_HELD);
        BigDecimal heldAmount = held.stream()
                .map(Payment::getAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal missing = heldAmount.subtract(availableForDebit(bank));
        return missing.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal availableForDebit(BankAccount sender) {
        return safe(sender.getBalance()).add(safe(sender.getDebtLimit()));
    }

    private void markBankWaitingForLiquidity(BankAccount bank) {
        if (bank.getOverlimitSince() == null) {
            bank.setOverlimitSince(LocalDateTime.now());
        }
        bankAccountRepository.save(bank);
    }

    private void refreshActualOverlimitMarker(BankAccount bank) {
        if (bank.isOverLimit()) {
            if (bank.getOverlimitSince() == null) {
                bank.setOverlimitSince(LocalDateTime.now());
            }
        } else if (!paymentRepository.existsBySenderBankIdAndStatus(bank.getBankId(), PaymentStatus.GRIDLOCK_HELD)) {
            bank.setOverlimitSince(null);
        }
    }

    private void refreshLiquidityMarkerAfterRetry(String bankId) {
        BankAccount bank = getBank(bankId);
        boolean stillHeld = paymentRepository.existsBySenderBankIdAndStatus(bankId, PaymentStatus.GRIDLOCK_HELD);
        if (stillHeld || bank.isOverLimit()) {
            if (bank.getOverlimitSince() == null) {
                bank.setOverlimitSince(LocalDateTime.now());
            }
        } else {
            bank.setOverlimitSince(null);
            if (bank.isBlocked()) {
                bank.setBlocked(false);
                bank.setBlockedAt(null);
                log.info("EXPRESS bank automatically unblocked after liquidity restored: {}", bankId);
            }
        }
        bankAccountRepository.save(bank);
    }

    private BankAccount getBank(String bankId) {
        return bankAccountRepository.findById(bankId)
                .orElseThrow(() -> new IllegalArgumentException("Nieznany bank: " + bankId));
    }

    private Map<String, Object> response(Payment payment, String message, BigDecimal requiredTopUp) {
        return Map.of(
                "paymentId", payment.getPaymentId(),
                "status", payment.getStatus().name(),
                "channel", SERVICE_CODE,
                "message", message,
                "requiredTopUp", formatAmount(requiredTopUp)
        );
    }

    private void validate(ExpressPaymentDto dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota musi być większa od zera.");
        }
        if (isBlank(dto.getCurrency())) {
            throw new IllegalArgumentException("Waluta jest wymagana.");
        }
        if (!ALLOWED_CURRENCIES.contains(dto.getCurrency())) {
            throw new IllegalArgumentException("Obsługiwana jest tylko waluta PLN.");
        }
        if (isBlank(dto.getSenderName())) {
            throw new IllegalArgumentException("Imię i nazwisko nadawcy jest wymagane.");
        }
        if (isBlank(dto.getReceiverName())) {
            throw new IllegalArgumentException("Imię i nazwisko odbiorcy jest wymagane.");
        }
        if (isBlank(dto.getSenderBankId()) || !ALLOWED_BANKS.contains(dto.getSenderBankId())) {
            throw new IllegalArgumentException("Nieprawidłowy bank nadawcy.");
        }
        if (isBlank(dto.getReceiverBankId()) || !ALLOWED_BANKS.contains(dto.getReceiverBankId())) {
            throw new IllegalArgumentException("Nieprawidłowy bank odbiorcy.");
        }
        if (dto.getSenderBankId().equals(dto.getReceiverBankId())) {
            throw new IllegalArgumentException("Bank nadawcy i odbiorcy nie mogą być takie same.");
        }
        if (!isValidSimpleIban(dto.getSenderAccount())) {
            throw new IllegalArgumentException("IBAN nadawcy powinien mieć format PL + 26 cyfr.");
        }
        if (!isValidSimpleIban(dto.getReceiverAccount())) {
            throw new IllegalArgumentException("IBAN odbiorcy powinien mieć format PL + 26 cyfr.");
        }
        if (normalizeIban(dto.getSenderAccount()).equals(normalizeIban(dto.getReceiverAccount()))) {
            throw new IllegalArgumentException("IBAN nadawcy i odbiorcy nie mogą być takie same.");
        }
        if (isBlank(dto.getTitle())) {
            throw new IllegalArgumentException("Tytuł przelewu jest wymagany.");
        }
        if (dto.getTitle().length() > 140) {
            throw new IllegalArgumentException("Tytuł przelewu może mieć maksymalnie 140 znaków.");
        }
    }

    private void sendLiquidityRequest(Payment payment, BankAccount sender, BigDecimal missingAmount) {
        String requestId = buildLiquidityRequestId(payment, sender);
        String payload = toLiquidityRequestXml(requestId, payment, sender, missingAmount);
        kafkaTemplate.send("liquidity.requests.express.sorbnet", requestId, payload);
        log.warn("EXPRESS liquidity request sent to SORBNET: requestId={}, paymentId={}, bank={}, amount={}",
                requestId, payment.getPaymentId(), sender.getBankId(), missingAmount);
    }

    private String buildLiquidityRequestId(Payment payment, BankAccount sender) {
        String id = payment.getPaymentId() == null ? UUID.randomUUID().toString() : payment.getPaymentId();
        String compact = id.replace("EXP-", "").replace("-", "");
        if (compact.length() > 12) {
            compact = compact.substring(0, 12);
        }
        return "LIQ-EXP-" + sender.getBankId() + "-" + compact;
    }

    private String toLiquidityRequestXml(String requestId, Payment payment, BankAccount sender, BigDecimal missingAmount) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document>
                    <LiquidityCreditTransferRequest>
                        <GrpHdr>
                            <MsgId>%s</MsgId>
                            <CreDtTm>%s</CreDtTm>
                        </GrpHdr>
                        <TrfInstr>
                            <ReqId>%s</ReqId>
                            <PaymentId>%s</PaymentId>
                            <BankId>%s</BankId>
                            <SourceServiceCode>SORBNET</SourceServiceCode>
                            <TargetServiceCode>EXPRESS</TargetServiceCode>
                            <SourceAccount>SORBNET-%s</SourceAccount>
                            <TargetAccount>EXPRESS-%s</TargetAccount>
                            <Amt Ccy="%s">%s</Amt>
                            <Reason>%s</Reason>
                            <CurrentBalance>%s</CurrentBalance>
                            <DebtLimit>%s</DebtLimit>
                            <AvailableForDebit>%s</AvailableForDebit>
                            <ApprovalStatus>PENDING_BANK_APPROVAL</ApprovalStatus>
                        </TrfInstr>
                    </LiquidityCreditTransferRequest>
                </Document>
                """.formatted(
                escapeXml(requestId),
                escapeXml(LocalDateTime.now().toString()),
                escapeXml(requestId),
                escapeXml(payment.getPaymentId()),
                escapeXml(sender.getBankId()),
                escapeXml(sender.getBankId()),
                escapeXml(sender.getBankId()),
                escapeXml(payment.getCurrency()),
                escapeXml(formatAmount(missingAmount)),
                escapeXml("Brak płynności w Elixir Express dla przelewu " + payment.getPaymentId()
                        + ". Minimalne zasilenie do limitu: " + formatAmount(missingAmount) + " PLN."),
                escapeXml(formatAmount(sender.getBalance())),
                escapeXml(formatAmount(sender.getDebtLimit())),
                escapeXml(formatAmount(availableForDebit(sender)))
        ).trim();
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidSimpleIban(String value) {
        return normalizeIban(value).matches("^PL\\d{26}$");
    }

    private String normalizeIban(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toUpperCase();
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}