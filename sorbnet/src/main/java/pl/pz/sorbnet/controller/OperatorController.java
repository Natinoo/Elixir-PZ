package pl.pz.sorbnet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.sorbnet.model.BankAccount;
import pl.pz.sorbnet.model.Payment;
import pl.pz.sorbnet.model.PaymentStatus;
import pl.pz.sorbnet.repository.BankAccountRepository;
import pl.pz.sorbnet.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sorbnet/operator")
public class OperatorController {

    private final BankAccountRepository accountRepo;
    private final PaymentRepository paymentRepo;

    public OperatorController(BankAccountRepository accountRepo,
                               PaymentRepository paymentRepo) {
        this.accountRepo = accountRepo;
        this.paymentRepo = paymentRepo;
    }

    @GetMapping("/banks")
    public List<BankAccount> allBanks() {
        return accountRepo.findAll();
    }

    @PostMapping("/banks/{bankId}/block")
    public ResponseEntity<Map<String, Object>> block(@PathVariable String bankId) {
        BankAccount bank = accountRepo.findById(bankId).orElseThrow();
        bank.setBlocked(true);
        bank.setBlockedAt(LocalDateTime.now());
        accountRepo.save(bank);
        return ResponseEntity.ok(Map.of("bankId", bankId, "status", "BLOCKED"));
    }

    @PostMapping("/banks/{bankId}/unblock")
    public ResponseEntity<Map<String, Object>> unblock(@PathVariable String bankId) {
        BankAccount bank = accountRepo.findById(bankId).orElseThrow();
        bank.setBlocked(false);
        bank.setBlockedAt(null);
        bank.setOverlimitSince(null);
        accountRepo.save(bank);
        return ResponseEntity.ok(Map.of("bankId", bankId, "status", "UNBLOCKED"));
    }

    @GetMapping("/emergencies")
    public List<BankAccount> emergencies() {
        return accountRepo.findOverLimit();
    }

    @GetMapping("/gridlock")
    public List<Payment> gridlockQueue() {
        return paymentRepo.findByStatus(PaymentStatus.GRIDLOCK_HELD);
    }

    @GetMapping("/payments")
    public List<Payment> allPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bankId) {

        if (status != null && bankId != null) {
            PaymentStatus ps = PaymentStatus.valueOf(status);
            return paymentRepo.findBySenderBankIdOrReceiverBankId(bankId, bankId)
                    .stream()
                    .filter(p -> p.getStatus() == ps)
                    .toList();
        }
        if (status != null) {
            return paymentRepo.findByStatus(PaymentStatus.valueOf(status));
        }
        if (bankId != null) {
            return paymentRepo.findBySenderBankIdOrReceiverBankId(bankId, bankId);
        }
        return paymentRepo.findAll();
    }

    @GetMapping("/payments/settled-today")
    public List<Payment> settledToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return paymentRepo.findByStatusAndSettledAtAfter(PaymentStatus.SETTLED, startOfDay);
    }
}