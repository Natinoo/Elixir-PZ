package pl.pz.elixirexpress.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pz.elixirexpress.model.Payment;
import pl.pz.elixirexpress.model.PaymentStatus;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findBySenderBankIdAndStatus(String senderBankId, PaymentStatus status);
}