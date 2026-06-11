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

    private final List<ElixirPaymentDto> currentSession = new ArrayList<>();
    private String pendingLiquiditySessionId;
    private List<LiquidityTransferRequestDto> pendingLiquidityRequests = new ArrayList<>();

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
            pendingLiquidityRequests = new ArrayList<>();
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
                pendingLiquidityRequests = List.copyOf(liquidityRequests);
                for (LiquidityTransferRequestDto request : liquidityRequests) {
                    kafkaTemplate.send("liquidity.requests.sorbnet", request.getRequestId(), toXml(request));
                    log.warn("Liquidity request sent to Sorbnet: requestId={}, bank={}, amount={}",
                            request.getRequestId(), request.getBankId(), request.getAmount());
                }
            } else {
                log.info("Session {} is still waiting for liquidity. Requests are not sent again.", sessionId);
            }

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
        pendingLiquidityRequests = new ArrayList<>();
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
        List<LiquidityTransferRequestDto> liquidityRequests = new ArrayList<>();

        for (NettingTransferDto transfer : transfers) {
            if (!bankLiquidityService.hasAvailableLiquidity(SERVICE_CODE, transfer.getDebtorBankId(), transfer.getAmount())) {
                BigDecimal requiredTopUp = bankLiquidityService.calculateRequiredTopUp(
                        SERVICE_CODE,
                        transfer.getDebtorBankId(),
                        transfer.getAmount()
                );
                liquidityRequests.add(buildLiquidityRequest(sessionId, transfer, requiredTopUp));
            }
        }

        return liquidityRequests;
    }

    private LiquidityTransferRequestDto buildLiquidityRequest(String sessionId,
                                                              NettingTransferDto transfer,
                                                              BigDecimal requiredTopUp) {
        String requestId = "LIQ-" + sessionId + "-" + transfer.getDebtorBankId();
        String bankId = transfer.getDebtorBankId();
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
                transfer.getCurrency(),
                "Brak płynności w ELIXIR przed lokalnym rozliczeniem sesji nettingowej",
                sourceHasFunds
        );
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
