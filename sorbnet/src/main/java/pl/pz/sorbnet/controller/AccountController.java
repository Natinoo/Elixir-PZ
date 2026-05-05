// controller/AccountController.java
package pl.pz.sorbnet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.sorbnet.model.BankAccount;
import pl.pz.sorbnet.repository.BankAccountRepository;
import pl.pz.sorbnet.service.GridlockResolutionService;
import pl.pz.sorbnet.service.SorbnetPaymentService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sorbnet/accounts")
public class AccountController {

     private final BankAccountRepository accountRepo;
    private final GridlockResolutionService gridlockService;
    private final SorbnetPaymentService paymentService; 

    public AccountController(BankAccountRepository accountRepo,
                              GridlockResolutionService gridlockService,
                              SorbnetPaymentService paymentService) { 
        this.accountRepo = accountRepo;
        this.gridlockService = gridlockService;
        this.paymentService = paymentService; 
    }


    @GetMapping
    public List<BankAccount> listAll() {
        return accountRepo.findAll();
    }

    @GetMapping("/{bankId}")
    public BankAccount get(@PathVariable String bankId) {
        return accountRepo.findById(bankId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono banku: " + bankId));
    }

    @GetMapping("/{bankId}/status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String bankId) {
        return ResponseEntity.ok(paymentService.getAccountStatus(bankId));
    }
    @PostMapping("/{bankId}/deposit")
    public ResponseEntity<Map<String, Object>> deposit(@PathVariable String bankId,
                                                        @RequestBody Map<String, BigDecimal> body) {
        BankAccount bank = accountRepo.findById(bankId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono banku: " + bankId));

        bank.setBalance(bank.getBalance().add(body.get("amount")));

        if (bank.getBalance().compareTo(bank.getDebtLimit().negate()) >= 0) {
            bank.setOverlimitSince(null);
        }
        accountRepo.save(bank);
        gridlockService.resolve(); // po zasileniu spróbuj odblokować kolejkę

        return ResponseEntity.ok(Map.of(
            "bankId", bankId,
            "newBalance", bank.getBalance(),
            "status", "DEPOSITED"
        ));
    }
}