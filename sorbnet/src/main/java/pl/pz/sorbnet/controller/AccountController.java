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
    public ResponseEntity<Map<String, Object>> deposit(
            @PathVariable String bankId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false, defaultValue = "NBP") String sourceBankId) {
        return ResponseEntity.ok(paymentService.simulateDeposit(bankId, amount, sourceBankId));
    }
}