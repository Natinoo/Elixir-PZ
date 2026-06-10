package pl.pz.elixir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pz.elixir.model.BankAccount;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
}