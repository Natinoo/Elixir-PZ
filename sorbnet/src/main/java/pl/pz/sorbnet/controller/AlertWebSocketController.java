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

    @MessageMapping("/alerts/{bankId}")
public void getAlert(@DestinationVariable String bankId) {
    BankAccount bank = accountRepo
            .findByServiceCodeAndBankId("SORBNET", bankId)
            .orElse(null);

    if (bank == null) {
        ws.convertAndSend("/topic/alerts/" + bankId, Map.of(
                "alert", false,
                "type", "BANK_NOT_FOUND",
                "message", "Nieznany bank: " + bankId
        ));
        return;
    }

    boolean overlimit = bank.getBalance()
            .compareTo(bank.getDebtLimit().negate()) < 0;

    Map<String, Object> response;

    if (bank.isBlocked()) {
        response = Map.of(
                "alert", true,
                "type", "BANK_BLOCKED",
                "bankId", bank.getBankId(),
                "bankName", bank.getBankName(),
                "balance", bank.getBalance(),
                "debtLimit", bank.getDebtLimit(),
                "blockedAt", bank.getBlockedAt() != null
                        ? bank.getBlockedAt().toString() : ""
        );
    } else if (overlimit) {
        response = Map.of(
                "alert", true,
                "type", "DEBT_LIMIT_EXCEEDED",
                "bankId", bank.getBankId(),
                "bankName", bank.getBankName(),
                "balance", bank.getBalance(),
                "debtLimit", bank.getDebtLimit(),
                "overlimitSince", bank.getOverlimitSince() != null
                        ? bank.getOverlimitSince().toString() : ""
        );
    } else {
        response = Map.of(
                "alert", false,
                "bankId", bank.getBankId(),
                "bankName", bank.getBankName(),
                "balance", bank.getBalance()
        );
    }

    ws.convertAndSend("/topic/alerts/" + bankId, response);
}
}