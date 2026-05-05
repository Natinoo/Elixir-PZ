package pl.pz.sorbnet.controller;

import org.springframework.web.bind.annotation.*;
import pl.pz.sorbnet.model.BankAccount;
import pl.pz.sorbnet.repository.BankAccountRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/sorbnet/alerts")
public class AlertController {

    private final BankAccountRepository accountRepo;

    public AlertController(BankAccountRepository accountRepo) {
        this.accountRepo = accountRepo;
    }

    // GUI banku polluje ten endpoint – jeśli bank przekroczył limit, dostaje alert
    @GetMapping("/{bankId}")
    public Map<String, Object> checkAlert(@PathVariable String bankId) {
        BankAccount bank = accountRepo.findById(bankId)
                .orElseThrow(() -> new RuntimeException("Nieznany bank: " + bankId));

        boolean overlimit = bank.getBalance()
                .compareTo(bank.getDebtLimit().negate()) < 0;

        if (bank.isBlocked()) {
            return Map.of(
                "alert", true,
                "type", "BANK_BLOCKED",
                "message", "Bank został automatycznie zablokowany z powodu utraty płynności.",
                "balance", bank.getBalance(),
                "debtLimit", bank.getDebtLimit()
            );
        }

        if (overlimit) {
            return Map.of(
                "alert", true,
                "type", "DEBT_LIMIT_EXCEEDED",
                "message", "Przekroczono limit zadłużenia. Należy niezwłocznie uzupełnić środki.",
                "balance", bank.getBalance(),
                "debtLimit", bank.getDebtLimit(),
                "overlimitSince", bank.getOverlimitSince().toString(),
                "blockedIfNotResolvedBy", bank.getOverlimitSince().plusHours(2).toString()
            );
        }

        return Map.of("alert", false, "balance", bank.getBalance());
    }
}