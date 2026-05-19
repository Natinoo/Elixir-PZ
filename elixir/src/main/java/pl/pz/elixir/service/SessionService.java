package pl.pz.elixir.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.PaymentStatus;

import java.util.ArrayList;
import java.util.List;

@Service
public class SessionService {

    private final List<ElixirPaymentDto> currentSession = new ArrayList<>();

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
    public void addToSession(String xml) {

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
        closeSession("TEST");
    }

    // zamknięcie sesji + netting
    public void closeSession(String sessionName) {

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

        // line = "BANK_A pays BANK_B = 250.0"
        try {
            String[] parts = line.split(" pays | = ");
            // parts[0] = "BANK_A", parts[1] = "BANK_B", parts[2] = "250.0"
            String sender   = parts[0].trim();
            String receiver = parts[1].trim();
            double amount   = Double.parseDouble(parts[2].trim());

            String paymentId = java.util.UUID.randomUUID().toString();

            // XML zgodny z @XmlRootElement(name = "ElixirPaymentDto") w SORBNET
            String xml = String.format(
                """
                <ElixirPaymentDto>\
                <paymentId>%s</paymentId>\
                <amount>%s</amount>\
                <currency>PLN</currency>\
                <senderAccount>%s</senderAccount>\
                <receiverAccount>%s</receiverAccount>\
                <title>Netting %s</title>\
                </ElixirPaymentDto>""",
                paymentId, amount, sender, receiver, sessionName
            );

            kafkaTemplate.send("payments.sorbnet", paymentId, xml);
            System.out.println("[ELIXIR→SORBNET] sent netting: " + line);

        } catch (Exception e) {
            System.err.println("[ELIXIR→SORBNET] parse error for line: " + line + " — " + e.getMessage());
        }
    }

    // update statusów surowych przelewów
    for (ElixirPaymentDto payment : currentSession) {
        elixirPaymentService.updatePaymentStatus(
                payment.getPaymentId(),
                PaymentStatus.PROCESSED
        );
    }

    currentSession.clear();
}
}