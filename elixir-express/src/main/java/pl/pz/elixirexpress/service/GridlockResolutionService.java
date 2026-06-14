package pl.pz.elixirexpress.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pz.elixirexpress.model.BankAccount;
import pl.pz.elixirexpress.model.Payment;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.repository.BankAccountRepository;
import pl.pz.elixirexpress.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GridlockResolutionService {

    private static final Logger log = LoggerFactory.getLogger(GridlockResolutionService.class);

    private final PaymentRepository paymentRepository;
    private final BankAccountRepository bankAccountRepository;
    private final ExpressPaymentService expressPaymentService;

    @Value("${elixir-express.overlimit.block-after-hours:2}")
    private int blockAfterHours;

    /**
     * Wygodne do demo. Jeżeli > 0, ma priorytet nad godzinami.
     * Przykład: elixir-express.overlimit.block-after-minutes=2
     */
    @Value("${elixir-express.overlimit.block-after-minutes:0}")
    private long blockAfterMinutes;

    public GridlockResolutionService(PaymentRepository paymentRepository,
                                     BankAccountRepository bankAccountRepository,
                                     ExpressPaymentService expressPaymentService) {
        this.paymentRepository = paymentRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.expressPaymentService = expressPaymentService;
    }

    @Scheduled(fixedDelayString = "${elixir-express.gridlock.interval-ms:30000}")
    @Transactional
    public void retryHeldPayments() {
        List<Payment> held = paymentRepository.findByStatus(PaymentStatus.GRIDLOCK_HELD);
        if (held.isEmpty()) {
            return;
        }

        int retried = 0;
        for (Payment payment : held) {
            if (expressPaymentService.retryHeldPayment(payment.getPaymentId())) {
                retried++;
            }
        }

        if (retried > 0) {
            log.info("EXPRESS gridlock retry resolved {} held payments", retried);
        }
    }

    @Scheduled(fixedDelayString = "${elixir-express.gridlock.interval-ms:30000}")
    @Transactional
    public void autoBlockOverlimitBanks() {
        LocalDateTime cutoff = blockAfterMinutes > 0
                ? LocalDateTime.now().minusMinutes(blockAfterMinutes)
                : LocalDateTime.now().minusHours(blockAfterHours);

        Set<String> candidateBankIds = new HashSet<>();

        for (Payment payment : paymentRepository.findByStatus(PaymentStatus.GRIDLOCK_HELD)) {
            candidateBankIds.add(payment.getSenderBankId());
        }

        for (BankAccount account : bankAccountRepository.findOverLimit()) {
            candidateBankIds.add(account.getBankId());
        }

        for (String bankId : candidateBankIds) {
            BankAccount bank = bankAccountRepository.findById(bankId).orElse(null);
            if (bank == null || bank.isBlocked()) {
                continue;
            }

            if (bank.getOverlimitSince() != null && bank.getOverlimitSince().isBefore(cutoff)) {
                bank.setBlocked(true);
                bank.setBlockedAt(LocalDateTime.now());
                bankAccountRepository.save(bank);

                log.warn("EXPRESS bank automatically blocked after liquidity timeout: bankId={}, balance={}, debtLimit={}, overlimitSince={}, timeout={}{}",
                        bank.getBankId(), bank.getBalance(), bank.getDebtLimit(), bank.getOverlimitSince(),
                        blockAfterMinutes > 0 ? blockAfterMinutes : blockAfterHours,
                        blockAfterMinutes > 0 ? " minutes" : " hours");
            }
        }
    }
}