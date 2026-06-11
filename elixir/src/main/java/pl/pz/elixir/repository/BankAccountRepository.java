package pl.pz.elixir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pz.elixir.model.BankAccount;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByServiceCodeAndBankId(String serviceCode, String bankId);

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    List<BankAccount> findByServiceCode(String serviceCode);
}