package pl.pz.sorbnet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pz.sorbnet.model.*;
import pl.pz.sorbnet.repository.*;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GridlockResolutionService {

    private static final Logger log = LoggerFactory.getLogger(GridlockResolutionService.class);

    private final PaymentRepository paymentRepo;
    private final BankAccountRepository accountRepo;

    @Value("${sorbnet.overlimit.block-after-hours:2}")
    private int blockAfterHours;

    private final SimpMessagingTemplate ws; // do pushowania alertów przez WebSocket

    public GridlockResolutionService(PaymentRepository paymentRepo,
                                  BankAccountRepository accountRepo,
                                  SimpMessagingTemplate ws) { 
    this.paymentRepo = paymentRepo;
    this.accountRepo = accountRepo;
    this.ws = ws;
    }
    /**
     * Algorytm gridlock resolution — uruchamia się co 60 sekund.
     * Gridlock = sytuacja gdy przelewy są wstrzymane bo wiele banków
     * jednocześnie czeka na środki od siebie nawzajem (zakleszczenie).
     *
     * Algorytm próbuje znaleźć kolejność rozliczenia przelewów
     * tak aby jak najwięcej z nich mogło przejść przy dostępnych saldach.
     */
    @Scheduled(fixedDelayString = "${sorbnet.gridlock.interval-ms:30000}")  
    @Transactional
    public void resolve() {
        // pobierz wszystkie przelewy czekające w kolejce gridlock
        List<Payment> held = paymentRepo.findByStatus(PaymentStatus.GRIDLOCK_HELD);
        if (held.isEmpty()) return;

        log.info("Gridlock resolution: {} przelewów oczekuje", held.size());
        boolean progress = true;
        // iteruj dopóki w danym cyklu udało się rozliczyć przynajmniej jeden przelew
        // (jeden rozliczony przelew może odblokować kolejny — efekt kaskadowy)
        while (progress) {
            progress = false;
             // pobiera świeżą listę z bazy w każdej iteracji
            for (Payment p : paymentRepo.findByStatus(PaymentStatus.GRIDLOCK_HELD)) {
                BankAccount sender = accountRepo.findById(p.getSenderBankId()).orElse(null);
                if (sender == null || sender.isBlocked()) continue;

                var newBalance = sender.getBalance().subtract(p.getAmount());
                if (newBalance.compareTo(sender.getDebtLimit().negate()) >= 0) {
                    BankAccount receiver = accountRepo.findById(p.getReceiverBankId()).orElse(null);
                    if (receiver == null) continue;

                    sender.setBalance(newBalance);
                    receiver.setBalance(receiver.getBalance().add(p.getAmount()));
                    sender.setOverlimitSince(null); 
                    accountRepo.saveAll(List.of(sender, receiver));

                    p.setStatus(PaymentStatus.SETTLED);
                    p.setSettledAt(LocalDateTime.now());
                    paymentRepo.save(p);

                    log.info("Gridlock rozwiązany dla przelewu {}", p.getPaymentId());
                    progress = true;
                }
            }
        }
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void autoBlock() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(blockAfterHours);  
        accountRepo.findOverLimit().stream()
         .filter(b -> !b.isBlocked())
            .filter(b -> b.getOverlimitSince() != null && b.getOverlimitSince().isBefore(cutoff))
            .forEach(b -> {
                b.setBlocked(true);
                b.setBlockedAt(LocalDateTime.now());
                accountRepo.save(b);
                log.warn("Bank {} automatycznie ZABLOKOWANY", b.getBankId());

                ws.convertAndSend("/topic/alerts/" + b.getBankId(), Map.of(
                    "alert", true,
                    "type", "BANK_BLOCKED",
                    "message", "Bank został automatycznie zablokowany z powodu utraty płynności.",
                    "balance", b.getBalance(),
                    "debtLimit", b.getDebtLimit()
                 ));
                // WebSocket push do GUI operatora 
                    ws.convertAndSend("/topic/operator/emergencies", Map.of(
                        "type", "BANK_BLOCKED",
                        "bankId", b.getBankId(),
                        "bankName", b.getBankName(),
                        "blockedAt", b.getBlockedAt().toString()
                    ));
            });
    }
    @Transactional
    public void unblockBank(String bankId) {
    BankAccount bank = accountRepo.findById(bankId)
            .orElseThrow(() -> new RuntimeException("Nieznany bank: " + bankId));

    bank.setBlocked(false);
    bank.setBlockedAt(null);
    bank.setOverlimitSince(null);
    accountRepo.save(bank);

    log.info("Bank {} odblokowany przez operatora", bankId);

    // poinformuj GUI banku że blokada została zdjęta
    ws.convertAndSend("/topic/alerts/" + bankId, Map.of(
        "alert", false,
        "type", "BANK_UNBLOCKED",
        "message", "Bank został odblokowany przez operatora NBP.",
        "balance", bank.getBalance(),
        "debtLimit", bank.getDebtLimit()
    ));

    // poinformuj GUI operatora
    ws.convertAndSend("/topic/operator/emergencies", Map.of(
        "type", "BANK_UNBLOCKED",
        "bankId", bank.getBankId(),
        "bankName", bank.getBankName()
    ));
}
        @Transactional
    public void blockBank(String bankId) {
        BankAccount bank = accountRepo.findById(bankId)
            .orElseThrow(() -> new RuntimeException("Nieznany bank: " + bankId));

        bank.setBlocked(true);
        bank.setBlockedAt(LocalDateTime.now());
        accountRepo.save(bank);

        log.warn("Bank {} zablokowany przez operatora", bankId);

        // poinformuj GUI banku
        ws.convertAndSend("/topic/alerts/" + bankId, Map.of(
        "alert", true,
        "type", "BANK_BLOCKED",
        "message", "Bank został zablokowany przez operatora NBP.",
        "balance", bank.getBalance(),
        "debtLimit", bank.getDebtLimit()
        ));

        // poinformuj GUI operatora
        ws.convertAndSend("/topic/operator/emergencies", Map.of(
        "type", "BANK_BLOCKED",
        "bankId", bank.getBankId(),
        "bankName", bank.getBankName(),
        "blockedAt", bank.getBlockedAt().toString()
        ));
}
}