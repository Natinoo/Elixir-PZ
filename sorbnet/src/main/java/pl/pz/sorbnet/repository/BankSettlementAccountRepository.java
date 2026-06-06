package pl.pz.sorbnet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pz.sorbnet.model.BankSettlementAccount;

import java.util.Optional;

public interface BankSettlementAccountRepository extends JpaRepository<BankSettlementAccount, Long> {

    Optional<BankSettlementAccount> findByBankIdAndIsDefaultTrue(String bankId);
    
    Optional<BankSettlementAccount> findByAccountNumber(String accountNumber);
}