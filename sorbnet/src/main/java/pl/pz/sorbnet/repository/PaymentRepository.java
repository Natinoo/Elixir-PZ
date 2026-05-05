package pl.pz.sorbnet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pz.sorbnet.model.Payment;
import pl.pz.sorbnet.model.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findBySenderBankIdOrReceiverBankId(String senderId, String receiverId);

    List<Payment> findBySenderBankIdOrReceiverBankIdAndCreatedAtBetween(
        String senderId, String receiverId,
        LocalDateTime from, LocalDateTime to
    );

}