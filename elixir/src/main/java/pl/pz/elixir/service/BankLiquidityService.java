package pl.pz.elixir.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pz.elixir.model.BankAccount;
import pl.pz.elixir.repository.BankAccountRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BankLiquidityService {

    public static final String ELIXIR = "ELIXIR";
    public static final String SORBNET = "SORBNET";

    private static final Logger log = LoggerFactory.getLogger(BankLiquidityService.class);

    private final BankAccountRepository bankAccountRepository;

    public BankLiquidityService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public boolean isBlocked(String bankId) {
        return isBlocked(ELIXIR, bankId);
    }

    public boolean isBlocked(String serviceCode, String bankId) {
        return bankAccountRepository.findByServiceCodeAndBankId(serviceCode, bankId)
                .map(BankAccount::isBlocked)
                .orElse(true);
    }

    public String getAccountNumber(String serviceCode, String bankId) {
        return getAccount(serviceCode, bankId).getAccountNumber();
    }

    public BankAccount getAccount(String serviceCode, String bankId) {
        return bankAccountRepository.findByServiceCodeAndBankId(serviceCode, bankId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Brak konta dla banku " + bankId + " w serwisie " + serviceCode));
    }

    public boolean hasAvailableLiquidity(String serviceCode, String bankId, BigDecimal debitAmount) {
        BankAccount account = getAccount(serviceCode, bankId);
        BigDecimal balanceAfterDebit = account.getBalance().subtract(debitAmount);
        BigDecimal lowestAllowedBalance = account.getDebtLimit().negate();
        return !account.isBlocked() && balanceAfterDebit.compareTo(lowestAllowedBalance) >= 0;
    }

    public BigDecimal calculateRequiredTopUp(String serviceCode, String bankId, BigDecimal debitAmount) {
        BankAccount account = getAccount(serviceCode, bankId);
        BigDecimal balanceAfterDebit = account.getBalance().subtract(debitAmount);
        BigDecimal lowestAllowedBalance = account.getDebtLimit().negate();
        if (balanceAfterDebit.compareTo(lowestAllowedBalance) >= 0) {
            return BigDecimal.ZERO;
        }
        return lowestAllowedBalance.subtract(balanceAfterDebit);
    }

    public boolean sorbnetCanFund(String bankId, BigDecimal amount) {
        BankAccount sorbnet = getAccount(SORBNET, bankId);
        return !sorbnet.isBlocked() && sorbnet.getBalance().compareTo(amount) >= 0;
    }

    @Transactional
    public void applyTransaction(String senderId, String receiverId, Double amount) {
        applyTransaction(ELIXIR, senderId, receiverId, BigDecimal.valueOf(amount));
    }

    @Transactional
    public void applyTransaction(String serviceCode, String senderId, String receiverId, BigDecimal amount) {
        BankAccount sender = getAccount(serviceCode, senderId);
        BankAccount receiver = getAccount(serviceCode, receiverId);

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        refreshLimitMarkers(sender);
        refreshLimitMarkers(receiver);

        bankAccountRepository.save(sender);
        bankAccountRepository.save(receiver);

        log.info("Applied {} transaction: {} -> {}, amount={}, senderBalance={}, receiverBalance={}",
                serviceCode, senderId, receiverId, amount, sender.getBalance(), receiver.getBalance());
    }

    @Transactional
    public void transferBetweenServices(String sourceServiceCode, String targetServiceCode, String bankId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota transferu płynności musi być większa od zera.");
        }

        BankAccount source = getAccount(sourceServiceCode, bankId);
        BankAccount target = getAccount(targetServiceCode, bankId);

        if (source.isBlocked()) {
            throw new IllegalStateException("Konto źródłowe jest zablokowane: " + sourceServiceCode + "/" + bankId);
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Brak środków na koncie " + sourceServiceCode + " banku " + bankId);
        }

        source.setBalance(source.getBalance().subtract(amount));
        target.setBalance(target.getBalance().add(amount));

        refreshLimitMarkers(source);
        refreshLimitMarkers(target);

        bankAccountRepository.save(source);
        bankAccountRepository.save(target);

        log.info("Liquidity moved for bank {}: {} -> {}, amount={}, sourceBalance={}, targetBalance={}",
                bankId, sourceServiceCode, targetServiceCode, amount, source.getBalance(), target.getBalance());
    }

    @Transactional
    public void blockBank(String bankId) {
        blockBank(ELIXIR, bankId);
    }

    @Transactional
    public void blockBank(String serviceCode, String bankId) {
        BankAccount bank = getAccount(serviceCode, bankId);
        bank.setBlocked(true);
        bank.setBlockedAt(LocalDateTime.now());
        bankAccountRepository.save(bank);
        log.info("Bank {} blocked in service {}", bankId, serviceCode);
    }

    @Transactional
    public void unblockBank(String bankId) {
        unblockBank(ELIXIR, bankId);
    }

    @Transactional
    public void unblockBank(String serviceCode, String bankId) {
        BankAccount bank = getAccount(serviceCode, bankId);
        bank.setBlocked(false);
        bank.setBlockedAt(null);
        bank.setOverlimitSince(null);
        bankAccountRepository.save(bank);
        log.info("Bank {} unblocked in service {}", bankId, serviceCode);
    }

    @Transactional
    public void topUp(String bankId, Double amount) {
        topUp(ELIXIR, bankId, BigDecimal.valueOf(amount));
    }

    @Transactional
    public void topUp(String serviceCode, String bankId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota musi być większa od zera.");
        }
        BankAccount bank = getAccount(serviceCode, bankId);
        bank.setBalance(bank.getBalance().add(amount));
        refreshLimitMarkers(bank);
        bankAccountRepository.save(bank);
        log.info("Top-up {} banku {} o kwotę {}, nowe saldo: {}", serviceCode, bankId, amount, bank.getBalance());
    }

    public Map<String, BigDecimal> getBalances() {
        return getBalances(ELIXIR);
    }

    public Map<String, BigDecimal> getBalances(String serviceCode) {
        List<BankAccount> accounts = bankAccountRepository.findByServiceCode(serviceCode);
        return accounts.stream()
                .collect(Collectors.toMap(BankAccount::getBankId, BankAccount::getBalance));
    }

    public Map<String, Boolean> getBlockedBanks() {
        return getBlockedBanks(ELIXIR);
    }

    public Map<String, Boolean> getBlockedBanks(String serviceCode) {
        List<BankAccount> accounts = bankAccountRepository.findByServiceCode(serviceCode);
        return accounts.stream()
                .collect(Collectors.toMap(BankAccount::getBankId, BankAccount::isBlocked));
    }

    private void refreshLimitMarkers(BankAccount account) {
        BigDecimal lowestAllowedBalance = account.getDebtLimit().negate();
        if (account.getBalance().compareTo(lowestAllowedBalance) < 0) {
            if (account.getOverlimitSince() == null) {
                account.setOverlimitSince(LocalDateTime.now());
            }
            log.warn("Bank {} in service {} exceeded debt limit. Balance={}, lowestAllowed={}",
                    account.getBankId(), account.getServiceCode(), account.getBalance(), lowestAllowedBalance);
        } else {
            account.setOverlimitSince(null);
            if (account.getBalance().compareTo(lowestAllowedBalance) >= 0) {
                account.setBlocked(false);
                account.setBlockedAt(null);
            }
        }
    }
}