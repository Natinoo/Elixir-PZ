package pl.pz.elixir.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.PaymentStatus;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private final List<ElixirPaymentDto> currentSession = new ArrayList<>();
    @Value("${elixir.session.test-mode:false}")
    private boolean testModeEnabled;

    private final NettingService nettingService;

    private final XmlMapper xmlMapper = new XmlMapper();

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final SessionReportService sessionReportService;

    private final BankLiquidityService bankLiquidityService;

    private final ElixirPaymentService elixirPaymentService;

    public SessionService(NettingService nettingService,
                          KafkaTemplate<String, String> kafkaTemplate,
                          SessionReportService sessionReportService,
                          BankLiquidityService bankLiquidityService,
                          ElixirPaymentService elixirPaymentService) {

        this.nettingService = nettingService;
        this.kafkaTemplate = kafkaTemplate;
        this.sessionReportService = sessionReportService;
        this.bankLiquidityService = bankLiquidityService;
        this.elixirPaymentService = elixirPaymentService;
    }

    public List<ElixirPaymentDto> getCurrentSession() {
        return currentSession;
    }

    // dodawanie przelewów do sesji
    public synchronized  void addToSession(String xml) {

        try {

            ElixirPaymentDto payment =
                    xmlMapper.readValue(xml, ElixirPaymentDto.class);

            String sender = payment.getSenderAccount();

            // blokada banku
            if (bankLiquidityService.isBlocked(sender)) {

                System.out.println("❌ BLOCKED BANK: " + sender);

                elixirPaymentService.updatePaymentStatus(
                        payment.getPaymentId(),
                        PaymentStatus.BLOCKED
                );

                return;
            }

            // aktualizacja płynności
            bankLiquidityService.applyTransaction(
                    payment.getSenderAccount(),
                    payment.getReceiverAccount(),
                    payment.getAmount()
            );

            currentSession.add(payment);

            System.out.println(
                    "Added to session. Current size: "
                            + currentSession.size()
            );

        } catch (Exception e) {

            throw new RuntimeException("XML parse error", e);
        }
    }

    // REALNE SESJE ELIXIR

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

    // TRYB TESTOWY
    @Scheduled(fixedRateString = "${elixir.session.interval:600000}")
    public void testSession() {
        if (testModeEnabled) {
            closeSession("TEST");
        }
    }

    // zamknięcie sesji + netting
public synchronized void closeSession(String sessionName) {
    if (currentSession.isEmpty()) {
        log.info("Session {} is empty", sessionName);
        return;
    }

    log.info("Closing ELIXIR session: {}", sessionName);

    List<String> nettingResult = nettingService.calculateNetting(currentSession);
    sessionReportService.saveReport(sessionName, nettingResult);

    log.info("Netting result size for session {}: {}", sessionName, nettingResult.size());

    for (String line : nettingResult) {
        log.info("Netting line: {}", line);

        try {
            String[] parts = line.split(" pays | = ");
            String sender = parts[0].trim();
            String receiver = parts[1].trim();
            double amount = Double.parseDouble(parts[2].trim());

            String paymentId = java.util.UUID.randomUUID().toString();

            ElixirPaymentDto dto = new ElixirPaymentDto(
                    paymentId,
                    amount,
                    "PLN",
                    sender,
                    receiver,
                    "Netting " + sessionName
            );

            JAXBContext context = JAXBContext.newInstance(ElixirPaymentDto.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);

            StringWriter writer = new StringWriter();
            marshaller.marshal(dto, writer);
            String xml = writer.toString();

            log.info("Prepared XML for paymentId={}: {}", paymentId, xml);

            kafkaTemplate.send("payments.sorbnet", paymentId, xml)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka send failed for paymentId={}", paymentId, ex);
                        } else {
                            log.info("Kafka sent: topic={}, partition={}, offset={}, paymentId={}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset(),
                                    paymentId);
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to process netting line: {}", line, e);
        }
    }

    for (ElixirPaymentDto payment : currentSession) {
        elixirPaymentService.updatePaymentStatus(
                payment.getPaymentId(),
                PaymentStatus.PROCESSED
        );
    }

    currentSession.clear();
    log.info("Session {} closed and cleared", sessionName);
}
}