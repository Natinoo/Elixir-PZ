package pl.pz.elixir.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.model.SettlementBankAccount;
import pl.pz.elixir.repository.SettlementBankAccountRepository;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private final List<ElixirPaymentDto> currentSession = new ArrayList<>();

    @Value("${elixir.session.test-mode:false}")
    private boolean testModeEnabled;

    private final NettingService nettingService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SessionReportService sessionReportService;
    private final BankLiquidityService bankLiquidityService;
    private final ElixirPaymentService elixirPaymentService;
    private final JAXBContext jaxbContext;
    private final SettlementBankAccountRepository settlementBankAccountRepository;

    public SessionService(
            NettingService nettingService,
            KafkaTemplate<String, String> kafkaTemplate,
            SessionReportService sessionReportService,
            BankLiquidityService bankLiquidityService,
            ElixirPaymentService elixirPaymentService,
            SettlementBankAccountRepository settlementBankAccountRepository
    ) throws JAXBException {
        this.nettingService = nettingService;
        this.kafkaTemplate = kafkaTemplate;
        this.sessionReportService = sessionReportService;
        this.bankLiquidityService = bankLiquidityService;
        this.elixirPaymentService = elixirPaymentService;
        this.jaxbContext = JAXBContext.newInstance(ElixirPaymentDto.class);
        this.settlementBankAccountRepository = settlementBankAccountRepository;
    }

    public List<ElixirPaymentDto> getCurrentSession() {
        return currentSession;
    }

    public synchronized void addToSession(String xml) {
        log.info("addToSession invoked");
        log.info("Incoming XML: {}", xml);

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ElixirPaymentDto payment = (ElixirPaymentDto) unmarshaller.unmarshal(new StringReader(xml));
            log.info("Parsed paymentId: {}", payment.getPaymentId());

            String senderBankId = payment.getSenderBankId();
            String receiverBankId = payment.getReceiverBankId();

            if (bankLiquidityService.isBlocked(senderBankId)) {
                log.warn("BLOCKED BANK: {}", senderBankId);
                elixirPaymentService.updatePaymentStatus(payment.getPaymentId(), PaymentStatus.BLOCKED);
                return;
            }

            bankLiquidityService.applyTransaction(
                    senderBankId,
                    receiverBankId,
                    payment.getAmount()
            );

            currentSession.add(payment);
            log.info("Added to session. Current size: {}", currentSession.size());

        } catch (Exception e) {
            log.error("XML parse error in addToSession", e);
            throw new RuntimeException("XML parse error", e);
        }
    }

    @Scheduled(cron = "0 30 9 * * *")
    public void sessionMorning() {
        closeSession("MORNING");
    }

    @Scheduled(cron = "0 30 13 * * *")
    public void sessionNoon() {
        closeSession("NOON");
    }

    @Scheduled(cron = "0 0 16 * * *")
    public void sessionAfternoon() {
        closeSession("AFTERNOON");
    }

    @Scheduled(fixedRateString = "${elixir.session.interval:600000}")
    public void testSession() {
        if (testModeEnabled) {
            log.info("Test session triggered");
            closeSession("TEST");
        }
    }

    private String resolveSettlementAccount(String bankId) {
    return settlementBankAccountRepository.findByBankIdAndIsDefaultTrue(bankId)
            .map(SettlementBankAccount::getAccountNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Brak domyślnego rachunku rozliczeniowego dla banku: " + bankId
            ));
    }

    public synchronized void closeSession(String sessionName) {
        if (currentSession.isEmpty()) {
            log.info("=== {} SESSION EMPTY ===", sessionName);
            return;
        }

        log.info("=== CLOSING ELIXIR SESSION: {} ===", sessionName);

        List<String> nettingResult = nettingService.calculateNetting(currentSession);
        sessionReportService.saveReport(sessionName, nettingResult);

        log.info("=== NETTING RESULT ===");
        log.info("Netting result size={}", nettingResult.size());


        boolean allSentSuccessfully = true;

for (String line : nettingResult) {
    log.info(line);

    try {
        String[] parts = line.split(" pays | = ");
        String sender = parts[0].trim();
        String receiver = parts[1].trim();
        double amount = Double.parseDouble(parts[2].trim());

        ElixirPaymentDto settlementDto = new ElixirPaymentDto();
        settlementDto.setPaymentId(UUID.randomUUID().toString());
        settlementDto.setAmount(amount);
        settlementDto.setCurrency("PLN");
        settlementDto.setSenderBankId(sender);
        settlementDto.setReceiverBankId(receiver);
        settlementDto.setSenderAccount(resolveSettlementAccount(sender));
        settlementDto.setReceiverAccount(resolveSettlementAccount(receiver));
        settlementDto.setTitle("Netting " + sessionName);

        String settlementXml = toXml(settlementDto);
        log.info("Sending settlement XML to SORBNet: {}", settlementXml);

        kafkaTemplate.send(
                "payments.sorbnet",
                settlementDto.getPaymentId(),
                settlementXml
        );

        log.info("[ELIXIR->SORBNET] sent netting: {}", line);

        } catch (Exception e) {
            allSentSuccessfully = false;
            log.error("[ELIXIR->SORBNET] parse/send error for line: {}", line, e);
        }
        }

        for (ElixirPaymentDto payment : currentSession) {
            elixirPaymentService.updatePaymentStatus(
                    payment.getPaymentId(),
                    allSentSuccessfully ? PaymentStatus.PROCESSED : PaymentStatus.BLOCKED
            );
        }

        currentSession.clear();
        log.info("Session cleared");
    }

    private String toXml(ElixirPaymentDto paymentDto) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            marshaller.marshal(paymentDto, sw);
            return sw.toString();
        } catch (JAXBException e) {
            log.error("Cannot serialize settlement to XML", e);
            throw new RuntimeException("Cannot serialize settlement to XML", e);
        }
    }
}