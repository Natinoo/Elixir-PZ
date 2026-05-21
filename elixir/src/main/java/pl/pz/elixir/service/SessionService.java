package pl.pz.elixir.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.PaymentStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final List<ElixirPaymentDto> currentSession = new ArrayList<>();

    @Value("${elixir.session.test-mode:false}")
    private boolean testModeEnabled;

    private final NettingService nettingService;
    private final XmlMapper xmlMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SessionReportService sessionReportService;
    private final BankLiquidityService bankLiquidityService;
    private final ElixirPaymentService elixirPaymentService;

    public SessionService(
            NettingService nettingService,
            KafkaTemplate<String, String> kafkaTemplate,
            SessionReportService sessionReportService,
            BankLiquidityService bankLiquidityService,
            ElixirPaymentService elixirPaymentService
    ) {
        this.nettingService = nettingService;
        this.kafkaTemplate = kafkaTemplate;
        this.sessionReportService = sessionReportService;
        this.bankLiquidityService = bankLiquidityService;
        this.elixirPaymentService = elixirPaymentService;
        this.xmlMapper = new XmlMapper();
    }

    public List<ElixirPaymentDto> getCurrentSession() {
        return currentSession;
    }

    public synchronized void addToSession(String xml) {
        try {
            ElixirPaymentDto payment = xmlMapper.readValue(xml, ElixirPaymentDto.class);
            String sender = payment.getSenderAccount();

            if (bankLiquidityService.isBlocked(sender)) {
                System.out.println("BLOCKED BANK: " + sender);
                elixirPaymentService.updatePaymentStatus(payment.getPaymentId(), PaymentStatus.BLOCKED);
                return;
            }

            bankLiquidityService.applyTransaction(
                    payment.getSenderAccount(),
                    payment.getReceiverAccount(),
                    payment.getAmount()
            );

            currentSession.add(payment);

            System.out.println("Added to session. Current size: " + currentSession.size());

        } catch (Exception e) {
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
            closeSession("TEST");
        }
    }

    public synchronized void closeSession(String sessionName) {
        if (currentSession.isEmpty()) {
            System.out.println("=== " + sessionName + " SESSION EMPTY ===");
            return;
        }

        System.out.println("=== CLOSING ELIXIR SESSION: " + sessionName + " ===");

        List<String> nettingResult = nettingService.calculateNetting(currentSession);
        sessionReportService.saveReport(sessionName, nettingResult);

        System.out.println("=== NETTING RESULT ===");

        for (String line : nettingResult) {
            System.out.println(line);

            try {
                String[] parts = line.split(" pays | = ");
                String sender = parts[0].trim();
                String receiver = parts[1].trim();
                double amount = Double.parseDouble(parts[2].trim());

                ElixirPaymentDto settlementDto = new ElixirPaymentDto();
                settlementDto.setPaymentId(UUID.randomUUID().toString());
                settlementDto.setAmount(amount);
                settlementDto.setCurrency("PLN");
                settlementDto.setSenderAccount(sender);
                settlementDto.setReceiverAccount(receiver);
                settlementDto.setTitle("Netting " + sessionName);

                String settlementXml = toXml(settlementDto);

                kafkaTemplate.send(
                        "payments.sorbnet",
                        settlementDto.getPaymentId(),
                        settlementXml
                );

                System.out.println("[ELIXIR->SORBNET] sent netting: " + line);

            } catch (Exception e) {
                System.err.println("[ELIXIR->SORBNET] parse error for line: " + line + " - " + e.getMessage());
            }
        }

        for (ElixirPaymentDto payment : currentSession) {
            elixirPaymentService.updatePaymentStatus(
                    payment.getPaymentId(),
                    PaymentStatus.PROCESSED
            );
        }

        currentSession.clear();
    }

    private String toXml(ElixirPaymentDto paymentDto) {
        try {
            return xmlMapper.writeValueAsString(paymentDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize settlement to XML", e);
        }
    }
}