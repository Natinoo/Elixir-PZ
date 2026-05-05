package pl.pz.sorbnet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.pz.sorbnet.model.BankAccount;
import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {

    @Query("SELECT b FROM BankAccount b WHERE b.balance < -b.debtLimit AND b.blocked = false")
    List<BankAccount> findOverLimit();
}