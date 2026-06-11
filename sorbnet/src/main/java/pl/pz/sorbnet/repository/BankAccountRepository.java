package pl.pz.sorbnet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.pz.sorbnet.model.BankAccount;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByServiceCodeAndBankId(String serviceCode, String bankId);

    Optional<BankAccount> findByServiceCodeAndAccountNumber(String serviceCode, String accountNumber);

    List<BankAccount> findAllByBankId(String bankId);

    List<BankAccount> findAllByServiceCode(String serviceCode);

    @Query("SELECT b FROM BankAccount b WHERE b.serviceCode = 'SORBNET' AND b.overlimitSince IS NOT NULL")
    List<BankAccount> findOverLimit();

    List<BankAccount> findByServiceCodeAndBlockedTrue(String serviceCode);
}