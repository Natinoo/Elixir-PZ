package pl.pz.sorbnet.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import pl.pz.sorbnet.model.BankAccount;
import pl.pz.sorbnet.repository.BankAccountRepository;

import java.util.Map;

@Controller
public class AlertWebSocketController {

    private final BankAccountRepository accountRepo;
    private final SimpMessagingTemplate ws;

    public AlertWebSocketController(BankAccountRepository accountRepo,
                                     SimpMessagingTemplate ws) {
        this.accountRepo = accountRepo;
        this.ws = ws;
    }

    // GUI wysyła /app/alerts/{bankId} → dostaje aktualny stan
    @MessageMapping("/alerts/{bankId}")
    public void getAlert(@DestinationVariable String bankId) {
        BankAccount bank = accountRepo.findById(bankId).orElseThrow();

        boolean overlimit = bank.getBalance()
                .compareTo(bank.getDebtLimit().negate()) < 0;

        Map<String, Object> response;

        if (bank.isBlocked()) {
            response = Map.of(
                "alert", true,
                "type", "BANK_BLOCKED",
                "balance", bank.getBalance(),
                "debtLimit", bank.getDebtLimit()
            );
        } else if (overlimit) {
            response = Map.of(
                "alert", true,
                "type", "DEBT_LIMIT_EXCEEDED",
                "balance", bank.getBalance(),
                "debtLimit", bank.getDebtLimit(),
                "overlimitSince", bank.getOverlimitSince().toString()
            );
        } else {
            response = Map.of("alert", false, "balance", bank.getBalance());
        }

        // odpowiedź idzie z powrotem do topiku tego banku
        ws.convertAndSend("/topic/alerts/" + bankId, response);
    }
}