package pl.pz.elixir.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pz.elixir.model.BankAccount;
import pl.pz.elixir.repository.BankAccountRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BankLiquidityService {

    private static final Logger log = LoggerFactory.getLogger(BankLiquidityService.class);

    private final BankAccountRepository bankAccountRepository;

    public BankLiquidityService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public boolean isBlocked(String bankId) {
        return bankAccountRepository.findById(bankId)
                .map(BankAccount::isBlocked)
                .orElse(true); // jeśli bank nie istnieje, uznajemy za zablokowany
    }

    @Transactional
    public void applyTransaction(String senderId, String receiverId, Double amount) {
        BankAccount sender = bankAccountRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Bank nadawcy nie istnieje: " + senderId));
        BankAccount receiver = bankAccountRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Bank odbiorcy nie istnieje: " + receiverId));

        // Aktualizacja sald
        sender.setBalance(sender.getBalance() - amount);
        receiver.setBalance(receiver.getBalance() + amount);

        // Sprawdzenie limitu dla nadawcy
        if (sender.getBalance() < -sender.getDebtLimit()) {
            if (sender.getOverlimitSince() == null) {
                sender.setOverlimitSince(java.time.LocalDateTime.now());
            }
            // Sprawdzenie czy automatycznie zablokować (po 2 godzinach – logika w Sorbnet, tu tylko ostrzeżenie)
            log.warn("Bank {} przekroczył limit zadłużenia! Saldo: {}, limit: {}",
                    senderId, sender.getBalance(), -sender.getDebtLimit());
        } else {
            sender.setOverlimitSince(null);
        }

        bankAccountRepository.save(sender);
        bankAccountRepository.save(receiver);
    }

    @Transactional
    public void blockBank(String bankId) {
        BankAccount bank = bankAccountRepository.findById(bankId)
                .orElseThrow(() -> new RuntimeException("Nieznany bank: " + bankId));
        bank.setBlocked(true);
        bank.setBlockedAt(java.time.LocalDateTime.now());
        bankAccountRepository.save(bank);
        log.info("Bank {} zablokowany", bankId);
    }

    @Transactional
    public void unblockBank(String bankId) {
        BankAccount bank = bankAccountRepository.findById(bankId)
                .orElseThrow(() -> new RuntimeException("Nieznany bank: " + bankId));
        bank.setBlocked(false);
        bank.setBlockedAt(null);
        bank.setOverlimitSince(null);
        bankAccountRepository.save(bank);
        log.info("Bank {} odblokowany", bankId);
    }

    @Transactional
    public void topUp(String bankId, Double amount) {
        BankAccount bank = bankAccountRepository.findById(bankId)
                .orElseThrow(() -> new RuntimeException("Nieznany bank: " + bankId));
        bank.setBalance(bank.getBalance() + amount);
        
        // Jeśli saldo wróciło powyżej limitu, odblokuj
        if (bank.getBalance() >= -bank.getDebtLimit()) {
            bank.setBlocked(false);
            bank.setOverlimitSince(null);
            bank.setBlockedAt(null);
            log.info("Bank {} automatycznie odblokowany po top-up", bankId);
        }
        bankAccountRepository.save(bank);
        log.info("Top-up banku {} o kwotę {}, nowe saldo: {}", bankId, amount, bank.getBalance());
    }

    public Map<String, Double> getBalances() {
        List<BankAccount> accounts = bankAccountRepository.findAll();
        return accounts.stream()
                .collect(Collectors.toMap(
                        BankAccount::getBankId,
                        BankAccount::getBalance
                ));
    }

    public Map<String, Boolean> getBlockedBanks() {
        List<BankAccount> accounts = bankAccountRepository.findAll();
        return accounts.stream()
                .collect(Collectors.toMap(
                        BankAccount::getBankId,
                        BankAccount::isBlocked
                ));
    }
}