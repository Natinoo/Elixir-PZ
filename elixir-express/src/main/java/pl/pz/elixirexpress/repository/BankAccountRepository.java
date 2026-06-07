// package pl.pz.elixirexpress.repository;

// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import pl.pz.elixirexpress.model.BankAccount;
// import java.util.List;

// public interface BankAccountRepository extends JpaRepository<BankAccount, String> {

//     @Query("SELECT b FROM BankAccount b WHERE b.balance < (b.debtLimit * -1)")
//     List<BankAccount> findOverLimit();
// }