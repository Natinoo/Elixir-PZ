package pl.pz.sorbnet.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.pz.sorbnet.dto.LiquidityTransferRequestDto;
import pl.pz.sorbnet.dto.LiquidityTransferResponseDto;
import pl.pz.sorbnet.messeging.IntegrationResponseProducer;
import pl.pz.sorbnet.model.BankAccount;
import pl.pz.sorbnet.model.LiquidityRequest;
import pl.pz.sorbnet.model.LiquidityRequestStatus;
import pl.pz.sorbnet.model.Payment;
import pl.pz.sorbnet.model.PaymentStatus;
import pl.pz.sorbnet.repository.BankAccountRepository;
import pl.pz.sorbnet.repository.LiquidityRequestRepository;
import pl.pz.sorbnet.repository.PaymentRepository;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Obsługa requestów płynnościowych z ELIXIR / ELIXIR EXPRESS.
 *
 * Przepływ:
 * 1. ELIXIR wykrywa brak płynności w sesji i wysyła request (Kafka, ISO 20022).
 * 2. SORBNET zapisuje request jako PENDING i pokazuje go w GUI (WebSocket).
 * 3. Operator banku w GUI wyklikuje przelew -> execute() obciąża rachunek
 *    banku w SORBNET i wysyła odpowiedź SETTLED do serwisu źródłowego.
 * 4. ELIXIR po otrzymaniu odpowiedzi zasila techniczne konto banku
 *    i domyka sesję czekającą na płynność.
 */
@Service
@Transactional
public class LiquidityService {

    private static final Logger log = LoggerFactory.getLogger(LiquidityService.class);

    private final LiquidityRequestRepository liquidityRepo;
    private final BankAccountRepository accountRepo;
    private final PaymentRepository paymentRepo;
    private final IntegrationResponseProducer responseProducer;
    private final SimpMessagingTemplate ws;

    public LiquidityService(LiquidityRequestRepository liquidityRepo,
                            BankAccountRepository accountRepo,
                            PaymentRepository paymentRepo,
                            IntegrationResponseProducer responseProducer,
                            SimpMessagingTemplate ws) {
        this.liquidityRepo = liquidityRepo;
        this.accountRepo = accountRepo;
        this.paymentRepo = paymentRepo;
        this.responseProducer = responseProducer;
        this.ws = ws;
    }

    /**
     * Rejestruje request płynnościowy odebrany z Kafki i powiadamia GUI.
     * Idempotentnie: ponowny request o tym samym ReqId jest ignorowany
     * (ELIXIR i tak zabezpiecza się przed dublowaniem, ale tu mamy drugą linię).
     *
     * @param source serwis, z którego topicu przyszedł komunikat (ELIXIR / ELIXIR_EXPRESS)
     */
    public void registerRequest(LiquidityTransferRequestDto dto, String source) {
    String requestId = dto.getRequestId() != null && !dto.getRequestId().isBlank()
            ? dto.getRequestId()
            : UUID.randomUUID().toString();

    if (liquidityRepo.existsById(requestId)) {
        log.info("[LIQUIDITY][{}] request {} już zarejestrowany (idempotent)", source, requestId);
        return;
    }

    String serviceCode = dto.getTargetServiceCode() != null && !dto.getTargetServiceCode().isBlank()
            ? dto.getTargetServiceCode()
            : source;

    LiquidityRequest req = new LiquidityRequest();
    req.setRequestId(requestId);

    // ponieważ w DTO nie masz getMessageId(), na razie fallback:
    req.setOriginalMessageId(requestId);

    req.setSessionId(dto.getSessionId());
    req.setBankId(normalize(dto.getBankId()));
    req.setRequestingServiceCode(serviceCode);
    req.setSourceAccount(dto.getSourceAccount());
    req.setTargetAccount(dto.getTargetAccount());
    req.setAmount(dto.getAmount());
    req.setCurrency(dto.getCurrency() != null && !dto.getCurrency().isBlank() ? dto.getCurrency() : "PLN");
    req.setMessage(dto.getMessage());
    req.setSourceHasFunds(dto.getSourceHasFunds());
    req.setStatus(LiquidityRequestStatus.PENDING);
    req.setReceivedAt(LocalDateTime.now());

    liquidityRepo.save(req);

    log.info("[LIQUIDITY][{}] zarejestrowano request {} bank={} kwota={} {} sessionId={} sourceHasFunds={}",
            source, requestId, req.getBankId(), req.getAmount(), req.getCurrency(),
            req.getSessionId(), req.getSourceHasFunds());

    Map<String, Object> payload = new HashMap<>();
        payload.put("type", "LIQUIDITY_REQUEST");
        payload.put("requestId", requestId);
        payload.put("sessionId", req.getSessionId() != null ? req.getSessionId() : "");
        payload.put("bankId", String.valueOf(req.getBankId()));
        payload.put("requestingServiceCode", req.getRequestingServiceCode());
        payload.put("sourceAccount", req.getSourceAccount() != null ? req.getSourceAccount() : "");
        payload.put("targetAccount", req.getTargetAccount() != null ? req.getTargetAccount() : "");
        payload.put("amount", req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO);
        payload.put("currency", req.getCurrency());
        payload.put("sourceHasFunds", req.getSourceHasFunds() != null ? req.getSourceHasFunds() : false);
        payload.put("message", req.getMessage() != null ? req.getMessage() : "Brak płynności w sesji");
        payload.put("receivedAt", req.getReceivedAt().toString());

        ws.convertAndSend("/topic/liquidity", payload);

    ws.convertAndSend("/topic/alerts/" + req.getBankId(), Map.of(
            "alert", true,
            "type", "LIQUIDITY_REQUEST",
            "requestId", requestId,
            "requestingServiceCode", req.getRequestingServiceCode(),
            "amount", req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO,
            "message", "Serwis " + req.getRequestingServiceCode()
                    + " zgłosił brak płynności. Wykonaj przelew zasilający z poziomu panelu."
    ));
}

    /**
     * Operator banku wykonuje (wyklikuje) przelew zasilający techniczne konto
     * banku w serwisie ELIXIR. Obciąża rachunek banku w SORBNET.
     */
    public Map<String, Object> execute(String requestId) {
        LiquidityRequest req = getPending(requestId);

        BankAccount bank = accountRepo.findByServiceCodeAndBankId("SORBNET", req.getBankId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Nieznany bank: " + req.getBankId()));

        if (bank.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bank " + bank.getBankId() + " jest zablokowany — przelew płynnościowy niemożliwy.");
        }

        BigDecimal newBalance = bank.getBalance().subtract(req.getAmount());
        if (newBalance.compareTo(bank.getDebtLimit().negate()) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Brak środków na rachunku SORBNET banku " + bank.getBankId()
                            + " (saldo=" + bank.getBalance()
                            + ", limit zadłużenia=" + bank.getDebtLimit() + ").");
        }

        bank.setBalance(newBalance);
        accountRepo.save(bank);

        // Zapis przelewu płynnościowego w historii SORBNET
        Payment payment = new Payment();
        payment.setPaymentId("LIQ-" + UUID.randomUUID());
        payment.setSenderBankId(bank.getBankId());
        payment.setReceiverBankId(bank.getBankId()); // ten sam bank, inny system
        payment.setSenderAccount(req.getSourceAccount());
        payment.setReceiverAccount(req.getTargetAccount());
        payment.setAmount(req.getAmount());
        payment.setCurrency(req.getCurrency());
        payment.setTitle("Zasilenie płynnościowe konta technicznego w "
                + req.getRequestingServiceCode());
        payment.setSourceService(req.getRequestingServiceCode());
        payment.setStatus(PaymentStatus.SETTLED);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setSettledAt(LocalDateTime.now());
        paymentRepo.save(payment);

        req.setStatus(LiquidityRequestStatus.EXECUTED);
        req.setProcessedAt(LocalDateTime.now());
        req.setPaymentId(payment.getPaymentId());
        liquidityRepo.save(req);

        sendResponse(req, "SETTLED", "Przelew płynnościowy wykonany przez operatora banku",
                payment.getSettledAt().toString());

        ws.convertAndSend("/topic/liquidity", Map.of(
                "type", "LIQUIDITY_EXECUTED",
                "requestId", req.getRequestId(),
                "bankId", req.getBankId(),
                "paymentId", payment.getPaymentId(),
                "amount", req.getAmount(),
                "newBalance", bank.getBalance()
        ));

        log.info("[LIQUIDITY] request {} wykonany, paymentId={}, nowe saldo banku {}={}",
                req.getRequestId(), payment.getPaymentId(), bank.getBankId(), bank.getBalance());

        return Map.of(
                "requestId", req.getRequestId(),
                "status", req.getStatus().name(),
                "paymentId", payment.getPaymentId(),
                "bankId", bank.getBankId(),
                "amount", req.getAmount(),
                "newBalance", bank.getBalance()
        );
    }

    /** Operator odrzuca request płynnościowy. */
    public Map<String, Object> reject(String requestId, String reason) {
        LiquidityRequest req = getPending(requestId);

        req.setStatus(LiquidityRequestStatus.REJECTED);
        req.setProcessedAt(LocalDateTime.now());
        liquidityRepo.save(req);

        String message = reason != null && !reason.isBlank()
                ? reason
                : "Request odrzucony przez operatora banku";

        sendResponse(req, "REJECTED", message, null);

        ws.convertAndSend("/topic/liquidity", Map.of(
                "type", "LIQUIDITY_REJECTED",
                "requestId", req.getRequestId(),
                "bankId", req.getBankId(),
                "message", message
        ));

        return Map.of(
                "requestId", req.getRequestId(),
                "status", req.getStatus().name(),
                "message", message
        );
    }

    public List<LiquidityRequest> findPending() {
        return liquidityRepo.findByStatusOrderByReceivedAtAsc(LiquidityRequestStatus.PENDING);
    }

    public List<LiquidityRequest> findByBank(String bankId) {
        return liquidityRepo.findByBankIdOrderByReceivedAtDesc(normalize(bankId));
    }

    // ===== prywatne =====

    private LiquidityRequest getPending(String requestId) {
        LiquidityRequest req = liquidityRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Nie znaleziono requestu płynnościowego: " + requestId));

        if (req.getStatus() != LiquidityRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Request " + requestId + " został już przetworzony (status="
                            + req.getStatus() + ").");
        }
        return req;
    }

    private void sendResponse(LiquidityRequest req, String status, String message, String settledAt) {
        LiquidityTransferResponseDto response = new LiquidityTransferResponseDto(req.getRequestId());
        response.setBankId(req.getBankId());
        response.setSourceServiceCode("SORBNET");
        response.setTargetServiceCode(req.getRequestingServiceCode());
        response.setSourceAccount(req.getSourceAccount());
        response.setTargetAccount(req.getTargetAccount());
        response.setAmount(req.getAmount(), req.getCurrency());
        response.setStatus(status);
        response.setMessage(message);
        response.setSettledAt(settledAt);

        String xml = marshal(response);
        log.info("[LIQUIDITY][{}] response payload={}", req.getRequestingServiceCode(), xml);

        if ("ELIXIR_EXPRESS".equalsIgnoreCase(req.getRequestingServiceCode())) {
            responseProducer.sendToExpress(req.getRequestId(), xml);
        } else {
            responseProducer.sendToElixir(req.getRequestId(), xml);
        }
    }

    private static final JAXBContext LIQUIDITY_JAXB_CTX;
        static {
            try {
                LIQUIDITY_JAXB_CTX = JAXBContext.newInstance(LiquidityTransferResponseDto.class);
            } catch (JAXBException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

    private String marshal(LiquidityTransferResponseDto dto) {
            try {
                Marshaller marshaller = LIQUIDITY_JAXB_CTX.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                StringWriter sw = new StringWriter();
                marshaller.marshal(dto, sw);
                return sw.toString();
            } catch (Exception e) {
                throw new RuntimeException("XML marshal error (liquidity response)", e);
            }
        }

    private String normalize(String bankId) {
        return bankId == null ? null : bankId.trim().toUpperCase();
    }

    public List<LiquidityRequest> getAllRequests() {
    return liquidityRepo.findAll();
}
}