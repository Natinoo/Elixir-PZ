package pl.pz.elixir.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.dto.LiquidityTransferRequestDto;
import pl.pz.elixir.dto.NettingTransferDto;
import pl.pz.elixir.model.BankAccount;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.repository.PaymentRepository;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final String SERVICE_CODE = BankLiquidityService.ELIXIR;
    private static final List<String> BANKS = List.of("BANK_A", "BANK_B", "BANK_C");
    private static final Duration LIQUIDITY_REQUEST_RESEND_INTERVAL = Duration.ofSeconds(10);

    private final List<ElixirPaymentDto> currentSession = new ArrayList<>();
    private String pendingLiquiditySessionId;
    private final Map<String, PendingLiquidityRequest> pendingLiquidityRequestsByBank = new LinkedHashMap<>();

    private final NettingService nettingService;
    private final BankLiquidityService bankLiquidityService;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public SessionService(NettingService nettingService,
                          BankLiquidityService bankLiquidityService,
                          PaymentRepository paymentRepository,
                          KafkaTemplate<String, String> kafkaTemplate) {
        this.nettingService = nettingService;
        this.bankLiquidityService = bankLiquidityService;
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public synchronized void addToSession(String iso20022Xml) {
        ElixirPaymentDto payment = fromXml(iso20022Xml, ElixirPaymentDto.class);
        payment.setType(SERVICE_CODE);
        payment.ensureDefaults();
        currentSession.add(payment);

        paymentRepository.findById(payment.getPaymentId()).ifPresent(entity -> {
            entity.setStatus(PaymentStatus.IN_SESSION);
            paymentRepository.save(entity);
        });

        log.info("Payment added to Elixir session: paymentId={}, sender={}, receiver={}, amount={}",
                payment.getPaymentId(), payment.getSenderBankId(), payment.getReceiverBankId(), payment.getAmount());
    }

    @Transactional
    public synchronized SessionCloseResult closeSession() {
        if (currentSession.isEmpty()) {
            pendingLiquiditySessionId = null;
            pendingLiquidityRequestsByBank.clear();
            return new SessionCloseResult(null, "EMPTY_SESSION", List.of(), List.of());
        }

        String sessionId = pendingLiquiditySessionId != null
                ? pendingLiquiditySessionId
                : "ELIXIR-SESSION-" + UUID.randomUUID();

        List<NettingTransferDto> transfers = nettingService.calculateNettingTransfers(currentSession, sessionId, SERVICE_CODE);
        fillSettlementAccounts(transfers);

        List<LiquidityTransferRequestDto> liquidityRequests = buildLiquidityRequests(sessionId, transfers);
        List<String> paymentIds = currentSession.stream().map(ElixirPaymentDto::getPaymentId).toList();

        if (!liquidityRequests.isEmpty()) {
            markPayments(paymentIds, PaymentStatus.WAITING_FOR_LIQUIDITY, sessionId);

            if (pendingLiquiditySessionId == null) {
                pendingLiquiditySessionId = sessionId;
            }

            sendLiquidityRequestsIfNeeded(sessionId, liquidityRequests);

            return new SessionCloseResult(sessionId, "WAITING_FOR_LIQUIDITY", transfers, liquidityRequests);
        }

        for (NettingTransferDto transfer : transfers) {
            bankLiquidityService.applyTransaction(
                    SERVICE_CODE,
                    transfer.getDebtorBankId(),
                    transfer.getCreditorBankId(),
                    transfer.getAmount()
            );
            log.info("Netting settled locally in ELIXIR: transferId={}, debtor={}, creditor={}, amount={}",
                    transfer.getTransferId(), transfer.getDebtorBankId(), transfer.getCreditorBankId(), transfer.getAmount());
        }

        markPayments(paymentIds, PaymentStatus.PROCESSED, sessionId);
        currentSession.clear();
        pendingLiquiditySessionId = null;
        pendingLiquidityRequestsByBank.clear();
        return new SessionCloseResult(sessionId, "SETTLED_IN_ELIXIR", transfers, List.of());
    }

    public synchronized List<ElixirPaymentDto> getCurrentSessionSnapshot() {
        return List.copyOf(currentSession);
    }

    public synchronized Map<String, CurrentBankExposure> getCurrentBankExposures() {
        Map<String, BankAggregation> aggregations = new LinkedHashMap<>();
        for (String bankId : BANKS) {
            aggregations.put(bankId, new BankAggregation());
        }

        for (ElixirPaymentDto payment : currentSession) {
            BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
            aggregations.computeIfAbsent(payment.getSenderBankId(), ignored -> new BankAggregation()).outgoing =
                    aggregations.computeIfAbsent(payment.getSenderBankId(), ignored -> new BankAggregation()).outgoing.add(amount);
            aggregations.computeIfAbsent(payment.getReceiverBankId(), ignored -> new BankAggregation()).incoming =
                    aggregations.computeIfAbsent(payment.getReceiverBankId(), ignored -> new BankAggregation()).incoming.add(amount);
        }

        Map<String, CurrentBankExposure> result = new LinkedHashMap<>();
        for (Map.Entry<String, BankAggregation> entry : aggregations.entrySet()) {
            result.put(entry.getKey(), buildExposure(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public synchronized CurrentBankExposure getCurrentBankExposure(String bankId) {
        BankAggregation aggregation = new BankAggregation();
        for (ElixirPaymentDto payment : currentSession) {
            BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
            if (bankId.equals(payment.getSenderBankId())) {
                aggregation.outgoing = aggregation.outgoing.add(amount);
            }
            if (bankId.equals(payment.getReceiverBankId())) {
                aggregation.incoming = aggregation.incoming.add(amount);
            }
        }
        return buildExposure(bankId, aggregation);
    }

    private CurrentBankExposure buildExposure(String bankId, BankAggregation aggregation) {
        BankAccount account = bankLiquidityService.getAccount(SERVICE_CODE, bankId);
        BigDecimal balance = account.getBalance();
        BigDecimal debtLimit = account.getDebtLimit();
        BigDecimal lowestAllowedBalance = debtLimit.negate();
        BigDecimal netPosition = aggregation.incoming.subtract(aggregation.outgoing);
        BigDecimal currentObligation = netPosition.compareTo(BigDecimal.ZERO) < 0 ? netPosition.abs() : BigDecimal.ZERO;
        BigDecimal projectedBalance = balance.subtract(currentObligation);
        BigDecimal requiredTopUp = projectedBalance.compareTo(lowestAllowedBalance) < 0
                ? lowestAllowedBalance.subtract(projectedBalance)
                : BigDecimal.ZERO;
        boolean limitExceeded = projectedBalance.compareTo(lowestAllowedBalance) < 0;
        boolean liquidityRisk = limitExceeded || account.isBlocked();

        return new CurrentBankExposure(
                bankId,
                balance,
                debtLimit,
                lowestAllowedBalance,
                aggregation.incoming,
                aggregation.outgoing,
                netPosition,
                currentObligation,
                projectedBalance,
                requiredTopUp,
                account.isBlocked(),
                limitExceeded,
                liquidityRisk
        );
    }

    private List<LiquidityTransferRequestDto> buildLiquidityRequests(String sessionId, List<NettingTransferDto> transfers) {
        Map<String, BigDecimal> totalDebitByBank = new LinkedHashMap<>();
        Map<String, String> currencyByBank = new LinkedHashMap<>();

        for (NettingTransferDto transfer : transfers) {
            if (transfer.getDebtorBankId() == null || transfer.getDebtorBankId().isBlank()) {
                continue;
            }

            BigDecimal amount = transfer.getAmount() == null ? BigDecimal.ZERO : transfer.getAmount();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String bankId = transfer.getDebtorBankId();
            totalDebitByBank.merge(bankId, amount, BigDecimal::add);
            currencyByBank.putIfAbsent(bankId, transfer.getCurrency() == null ? "PLN" : transfer.getCurrency());
        }

        List<LiquidityTransferRequestDto> liquidityRequests = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : totalDebitByBank.entrySet()) {
            String bankId = entry.getKey();
            BigDecimal totalDebit = entry.getValue();

            if (!bankLiquidityService.hasAvailableLiquidity(SERVICE_CODE, bankId, totalDebit)) {
                BigDecimal requiredTopUp = bankLiquidityService.calculateRequiredTopUp(
                        SERVICE_CODE,
                        bankId,
                        totalDebit
                );

                liquidityRequests.add(buildLiquidityRequest(
                        sessionId,
                        bankId,
                        totalDebit,
                        currencyByBank.getOrDefault(bankId, "PLN"),
                        requiredTopUp
                ));
            }
        }

        return liquidityRequests;
    }

    private LiquidityTransferRequestDto buildLiquidityRequest(String sessionId,
                                                              String bankId,
                                                              BigDecimal totalDebit,
                                                              String currency,
                                                              BigDecimal requiredTopUp) {
        String requestId = buildShortLiquidityRequestId(sessionId, bankId);
        String sourceAccount = bankLiquidityService.getAccountNumber(BankLiquidityService.SORBNET, bankId);
        String targetAccount = bankLiquidityService.getAccountNumber(BankLiquidityService.ELIXIR, bankId);
        boolean sourceHasFunds = bankLiquidityService.sorbnetCanFund(bankId, requiredTopUp);

        return new LiquidityTransferRequestDto(
                requestId,
                sessionId,
                bankId,
                BankLiquidityService.SORBNET,
                BankLiquidityService.ELIXIR,
                sourceAccount,
                targetAccount,
                requiredTopUp,
                currency,
                "Brak płynności w ELIXIR przed lokalnym rozliczeniem sesji nettingowej. "
                        + "Łączne zobowiązanie banku w sesji: " + totalDebit,
                sourceHasFunds
        );
    }

    private String buildShortLiquidityRequestId(String sessionId, String bankId) {
        String safeBankId = bankId == null || bankId.isBlank() ? "BANK" : bankId.replaceAll("[^A-Za-z0-9_]", "_");
        String sessionPart = sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;

        int lastDash = sessionPart.lastIndexOf('-');
        if (lastDash >= 0 && lastDash < sessionPart.length() - 1) {
            sessionPart = sessionPart.substring(lastDash + 1);
        }
        if (sessionPart.length() > 12) {
            sessionPart = sessionPart.substring(sessionPart.length() - 12);
        }

        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "LIQ-" + safeBankId + "-" + sessionPart + "-" + randomPart;
    }

    private void sendLiquidityRequestsIfNeeded(String sessionId, List<LiquidityTransferRequestDto> liquidityRequests) {
        LocalDateTime now = LocalDateTime.now();
        List<String> banksStillRequiringLiquidity = liquidityRequests.stream()
                .map(LiquidityTransferRequestDto::getBankId)
                .filter(bankId -> bankId != null && !bankId.isBlank())
                .distinct()
                .toList();

        pendingLiquidityRequestsByBank.entrySet().removeIf(entry -> !banksStillRequiringLiquidity.contains(entry.getKey()));

        for (LiquidityTransferRequestDto request : liquidityRequests) {
            String bankId = request.getBankId();
            if (bankId == null || bankId.isBlank()) {
                log.warn("Liquidity request skipped: missing bankId. sessionId={}, requestId={}",
                        sessionId, request.getRequestId());
                continue;
            }

            PendingLiquidityRequest pending = pendingLiquidityRequestsByBank.get(bankId);
            if (pending == null) {
                PendingLiquidityRequest newPending = new PendingLiquidityRequest(request);
                pendingLiquidityRequestsByBank.put(bankId, newPending);
                sendLiquidityRequest(sessionId, newPending, "new request");
                continue;
            }

            boolean amountChanged = compareAmount(pending.request.getAmount(), request.getAmount()) != 0;
            if (amountChanged) {
                PendingLiquidityRequest replacement = new PendingLiquidityRequest(request);
                pendingLiquidityRequestsByBank.put(bankId, replacement);
                sendLiquidityRequest(sessionId, replacement, "required amount changed");
                continue;
            }

            boolean resendDue = pending.lastSentAt == null
                    || Duration.between(pending.lastSentAt, now).compareTo(LIQUIDITY_REQUEST_RESEND_INTERVAL) >= 0;
            if (resendDue) {
                sendLiquidityRequest(sessionId, pending, "pending request resend");
            } else {
                log.info(
                        "Session {} is still waiting for liquidity from bank {}. Request is pending; resend not due yet. requestId={}, amount={}, sentCount={}",
                        sessionId,
                        bankId,
                        pending.request.getRequestId(),
                        pending.request.getAmount(),
                        pending.sendCount
                );
            }
        }
    }

    private void sendLiquidityRequest(String sessionId, PendingLiquidityRequest pending, String reason) {
        kafkaTemplate.send("liquidity.requests.sorbnet", pending.request.getRequestId(), toXml(pending.request));
        pending.lastSentAt = LocalDateTime.now();
        pending.sendCount++;
        log.warn("Liquidity request sent to Sorbnet: sessionId={}, requestId={}, bank={}, amount={}, reason={}, sentCount={}",
                sessionId,
                pending.request.getRequestId(),
                pending.request.getBankId(),
                pending.request.getAmount(),
                reason,
                pending.sendCount);
    }

    private int compareAmount(BigDecimal left, BigDecimal right) {
        BigDecimal safeLeft = left == null ? BigDecimal.ZERO : left;
        BigDecimal safeRight = right == null ? BigDecimal.ZERO : right;
        return safeLeft.compareTo(safeRight);
    }

    public synchronized void markLiquidityRequestCompleted(String requestId, String bankId) {
        if (pendingLiquidityRequestsByBank.isEmpty()) {
            return;
        }

        List<String> banksToRemove = pendingLiquidityRequestsByBank.entrySet().stream()
                .filter(entry -> matchesCompletedLiquidityRequest(entry.getValue().request, requestId, bankId))
                .map(Map.Entry::getKey)
                .toList();

        for (String bankToRemove : banksToRemove) {
            PendingLiquidityRequest removed = pendingLiquidityRequestsByBank.remove(bankToRemove);
            if (removed != null) {
                log.info("Liquidity request completed in ELIXIR session: requestId={}, bank={}",
                        removed.request.getRequestId(), removed.request.getBankId());
            }
        }
    }

    private boolean matchesCompletedLiquidityRequest(LiquidityTransferRequestDto request, String requestId, String bankId) {
        if (requestId != null && !requestId.isBlank() && requestId.equals(request.getRequestId())) {
            return true;
        }
        return bankId != null && !bankId.isBlank() && bankId.equals(request.getBankId());
    }

    private void fillSettlementAccounts(List<NettingTransferDto> transfers) {
        for (NettingTransferDto transfer : transfers) {
            transfer.setDebtorAccount(bankLiquidityService.getAccountNumber(SERVICE_CODE, transfer.getDebtorBankId()));
            transfer.setCreditorAccount(bankLiquidityService.getAccountNumber(SERVICE_CODE, transfer.getCreditorBankId()));
        }
    }

    private void markPayments(List<String> paymentIds, PaymentStatus status, String sessionId) {
        for (String paymentId : paymentIds) {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment != null) {
                payment.setStatus(status);
                payment.setSessionId(sessionId);
                paymentRepository.save(payment);
            }
        }
    }

    private <T> T fromXml(String xml, Class<T> clazz) {
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            JAXBElement<T> root = unmarshaller.unmarshal(new StreamSource(new StringReader(xml)), clazz);
            return root.getValue();
        } catch (Exception e) {
            throw new IllegalArgumentException("Nie można odczytać ISO 20022 XML: " + e.getMessage(), e);
        }
    }

    private String toXml(Object dto) {
        try {
            JAXBContext context = JAXBContext.newInstance(dto.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            marshaller.marshal(dto, sw);
            return sw.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Nie można zapisać ISO 20022 XML: " + e.getMessage(), e);
        }
    }

    private static class PendingLiquidityRequest {
        private final LiquidityTransferRequestDto request;
        private LocalDateTime lastSentAt;
        private int sendCount;

        private PendingLiquidityRequest(LiquidityTransferRequestDto request) {
            this.request = request;
        }
    }

    private static class BankAggregation {
        private BigDecimal incoming = BigDecimal.ZERO;
        private BigDecimal outgoing = BigDecimal.ZERO;
    }

    public static class CurrentBankExposure {
        private final String bankId;
        private final BigDecimal balance;
        private final BigDecimal debtLimit;
        private final BigDecimal lowestAllowedBalance;
        private final BigDecimal incomingAmount;
        private final BigDecimal outgoingAmount;
        private final BigDecimal netPosition;
        private final BigDecimal currentObligation;
        private final BigDecimal projectedBalanceAfterObligation;
        private final BigDecimal requiredTopUp;
        private final boolean blocked;
        private final boolean limitExceeded;
        private final boolean liquidityRisk;

        public CurrentBankExposure(String bankId,
                                   BigDecimal balance,
                                   BigDecimal debtLimit,
                                   BigDecimal lowestAllowedBalance,
                                   BigDecimal incomingAmount,
                                   BigDecimal outgoingAmount,
                                   BigDecimal netPosition,
                                   BigDecimal currentObligation,
                                   BigDecimal projectedBalanceAfterObligation,
                                   BigDecimal requiredTopUp,
                                   boolean blocked,
                                   boolean limitExceeded,
                                   boolean liquidityRisk) {
            this.bankId = bankId;
            this.balance = balance;
            this.debtLimit = debtLimit;
            this.lowestAllowedBalance = lowestAllowedBalance;
            this.incomingAmount = incomingAmount;
            this.outgoingAmount = outgoingAmount;
            this.netPosition = netPosition;
            this.currentObligation = currentObligation;
            this.projectedBalanceAfterObligation = projectedBalanceAfterObligation;
            this.requiredTopUp = requiredTopUp;
            this.blocked = blocked;
            this.limitExceeded = limitExceeded;
            this.liquidityRisk = liquidityRisk;
        }

        public String getBankId() {
            return bankId;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public BigDecimal getDebtLimit() {
            return debtLimit;
        }

        public BigDecimal getLowestAllowedBalance() {
            return lowestAllowedBalance;
        }

        public BigDecimal getIncomingAmount() {
            return incomingAmount;
        }

        public BigDecimal getOutgoingAmount() {
            return outgoingAmount;
        }

        public BigDecimal getNetPosition() {
            return netPosition;
        }

        public BigDecimal getCurrentObligation() {
            return currentObligation;
        }

        public BigDecimal getProjectedBalanceAfterObligation() {
            return projectedBalanceAfterObligation;
        }

        public BigDecimal getRequiredTopUp() {
            return requiredTopUp;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public boolean isLimitExceeded() {
            return limitExceeded;
        }

        public boolean isLiquidityRisk() {
            return liquidityRisk;
        }
    }

    public static class SessionCloseResult {
        private final String sessionId;
        private final String status;
        private final List<NettingTransferDto> nettingTransfers;
        private final List<LiquidityTransferRequestDto> liquidityRequests;

        public SessionCloseResult(String sessionId, String status, List<NettingTransferDto> nettingTransfers,
                                  List<LiquidityTransferRequestDto> liquidityRequests) {
            this.sessionId = sessionId;
            this.status = status;
            this.nettingTransfers = nettingTransfers;
            this.liquidityRequests = liquidityRequests;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getStatus() {
            return status;
        }

        public List<NettingTransferDto> getNettingTransfers() {
            return nettingTransfers;
        }

        public List<LiquidityTransferRequestDto> getLiquidityRequests() {
            return liquidityRequests;
        }
    }
}
