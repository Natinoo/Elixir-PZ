package pl.pz.elixir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pz.elixir.model.SettlementBankAccount;

import java.util.Optional;

public interface SettlementBankAccountRepository extends JpaRepository<SettlementBankAccount, Long> {

    Optional<SettlementBankAccount> findByServiceCodeAndBankIdAndIsDefaultTrue(String serviceCode, String bankId);
}