package pl.pz.elixir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;

import java.util.Collection;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByServiceCodeAndStatus(String serviceCode, PaymentStatus status);

    List<Payment> findBySenderBankIdAndStatus(String senderBankId, PaymentStatus status);

    List<Payment> findBySessionId(String sessionId);

    List<Payment> findBySessionIdAndStatusIn(String sessionId, Collection<PaymentStatus> statuses);
}