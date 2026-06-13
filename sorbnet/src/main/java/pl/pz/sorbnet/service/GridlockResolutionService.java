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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class GridlockResolutionService {

    private static final Logger log = LoggerFactory.getLogger(GridlockResolutionService.class);

    private final PaymentRepository paymentRepo;
    private final BankAccountRepository accountRepo;
    private final SimpMessagingTemplate ws;

    @Value("${sorbnet.overlimit.block-after-hours:2}")
    private int blockAfterHours;

    @Value("${sorbnet.overlimit.block-after-minutes:0}")
    private long blockAfterMinutes;

    public GridlockResolutionService(PaymentRepository paymentRepo,
                                     BankAccountRepository accountRepo,
                                     SimpMessagingTemplate ws) {
        this.paymentRepo = paymentRepo;
        this.accountRepo = accountRepo;
        this.ws = ws;
    }

    @Scheduled(fixedDelayString = "${sorbnet.gridlock.interval-ms:30000}")
    @Transactional
    public void resolve() {
        List<Payment> held = paymentRepo.findByStatus(PaymentStatus.GRIDLOCK_HELD);
        if (held.isEmpty()) return;

        log.info("Gridlock resolution: {} przelewów oczekuje", held.size());
        boolean progress = true;

        while (progress) {
            progress = false;
            for (Payment p : paymentRepo.findByStatus(PaymentStatus.GRIDLOCK_HELD)) {

                BankAccount sender = accountRepo
                        .findByServiceCodeAndBankId("SORBNET", p.getSenderBankId())
                        .orElse(null);
                if (sender == null || sender.isBlocked()) continue;

                var newBalance = sender.getBalance().subtract(p.getAmount());
                if (newBalance.compareTo(sender.getDebtLimit().negate()) >= 0) {

                    BankAccount receiver = accountRepo
                            .findByServiceCodeAndBankId("SORBNET", p.getReceiverBankId())
                            .orElse(null);
                    if (receiver == null) continue;

                        sender.setBalance(newBalance);

                        receiver.setBalance(receiver.getBalance().add(p.getAmount()));

                        p.setStatus(PaymentStatus.SETTLED);
                        p.setSettledAt(LocalDateTime.now());
                        paymentRepo.save(p);

                        // flagę zdejmujemy dopiero, gdy bank nie ma już ŻADNEGO wstrzymanego przelewu
                        boolean stillHeld = paymentRepo
                                .findBySenderBankIdAndStatus(sender.getBankId(), PaymentStatus.GRIDLOCK_HELD)
                                .stream()
                                .anyMatch(h -> !h.getPaymentId().equals(p.getPaymentId()));
                        if (!stillHeld) {
                        sender.setOverlimitSince(null);
                        }
                        accountRepo.saveAll(List.of(sender, receiver));
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${sorbnet.overlimit.check-interval-ms:60000}")
    @Transactional
    public void autoBlock() {
         LocalDateTime cutoff = blockAfterMinutes > 0 
                ? LocalDateTime.now().minusMinutes(blockAfterMinutes)
                : LocalDateTime.now().minusHours(blockAfterHours);
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
        BankAccount bank = accountRepo
                .findByServiceCodeAndBankId("SORBNET", bankId)
                .orElseThrow(() -> new RuntimeException("Nieznany bank: " + bankId));

        bank.setBlocked(false);
        bank.setBlockedAt(null);
        bank.setOverlimitSince(null);
        accountRepo.save(bank);

        log.info("Bank {} odblokowany przez operatora", bankId);

        ws.convertAndSend("/topic/alerts/" + bankId, Map.of(
                "alert", false,
                "type", "BANK_UNBLOCKED",
                "message", "Bank został odblokowany przez operatora NBP.",
                "balance", bank.getBalance(),
                "debtLimit", bank.getDebtLimit()
        ));
        ws.convertAndSend("/topic/operator/emergencies", Map.of(
                "type", "BANK_UNBLOCKED",
                "bankId", bank.getBankId(),
                "bankName", bank.getBankName()
        ));
    }

    @Transactional
    public void blockBank(String bankId) {
        BankAccount bank = accountRepo
                .findByServiceCodeAndBankId("SORBNET", bankId)
                .orElseThrow(() -> new RuntimeException("Nieznany bank: " + bankId));

        bank.setBlocked(true);
        bank.setBlockedAt(LocalDateTime.now());
        accountRepo.save(bank);

        log.warn("Bank {} zablokowany przez operatora", bankId);

        ws.convertAndSend("/topic/alerts/" + bankId, Map.of(
                "alert", true,
                "type", "BANK_BLOCKED",
                "message", "Bank został zablokowany przez operatora NBP.",
                "balance", bank.getBalance(),
                "debtLimit", bank.getDebtLimit()
        ));
        ws.convertAndSend("/topic/operator/emergencies", Map.of(
                "type", "BANK_BLOCKED",
                "bankId", bank.getBankId(),
                "bankName", bank.getBankName(),
                "blockedAt", bank.getBlockedAt().toString()
        ));
    }
}