package pl.pz.elixir.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import pl.pz.elixir.dto.ElixirPaymentDto;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

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

    public SessionService(NettingService nettingService,
                          KafkaTemplate<String, String> kafkaTemplate,
                          SessionReportService sessionReportService,
                          BankLiquidityService bankLiquidityService) {

        this.nettingService = nettingService;
        this.kafkaTemplate = kafkaTemplate;
        this.sessionReportService = sessionReportService;
        this.bankLiquidityService = bankLiquidityService;
    }

    // dodawanie przelewów do sesji
    public void addToSession(String xml) {

        try {

            ElixirPaymentDto payment =
                    xmlMapper.readValue(xml, ElixirPaymentDto.class);

            String sender = payment.getSenderAccount();

            // blokada banku
            if (bankLiquidityService.isBlocked(sender)) {

                System.out.println(
                        "❌ BLOCKED BANK: " + sender
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
    @Scheduled(fixedRateString = "${elixir.session.interval}")
    public void testSession() {
        closeSession("TEST");
    }

    // zamknięcie sesji + netting
    public void closeSession(String sessionName) {

        if (currentSession.isEmpty()) {

            System.out.println(
                    "=== " + sessionName + " SESSION EMPTY ==="
            );

            return;
        }

        System.out.println(
                "=== CLOSING ELIXIR SESSION: "
                        + sessionName
                        + " ==="
        );

        var result =
                nettingService.calculateNetting(currentSession);

        // zapis raportu
        sessionReportService.saveReport(result);

        System.out.println("=== NETTING RESULT ===");

        for (String line : result) {

            System.out.println(line);

            // wysyłka do Sorbnet
            kafkaTemplate.send(
                    "payments.sorbnet.settlement",
                    line
            );
        }

        currentSession.clear();
    }
}