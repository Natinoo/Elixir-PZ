package pl.pz.sorbnet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pl.pz.sorbnet.model.Payment;
import pl.pz.sorbnet.model.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findBySenderBankIdOrReceiverBankId(String senderId, String receiverId);

    @Query("""
    SELECT p FROM Payment p
    WHERE (p.senderBankId = :bankId OR p.receiverBankId = :bankId)
      AND p.createdAt BETWEEN :from AND :to
    ORDER BY p.createdAt DESC
    """)
    List<Payment> findByBankIdAndFromBetween(
    @Param("bankId") String bankId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
    );
    List<Payment> findByStatusAndSettledAtAfter(PaymentStatus status, LocalDateTime after);
    @Query("SELECT p FROM Payment p WHERE (p.senderBankId = :bankId OR p.receiverBankId = :bankId) AND p.createdAt >= :from ORDER BY p.createdAt DESC")
    List<Payment> findByBankIdAndFrom(@Param("bankId") String bankId,
                                      @Param("from") LocalDateTime from);

    @Query("SELECT p FROM Payment p WHERE (p.senderBankId = :bankId OR p.receiverBankId = :bankId) ORDER BY p.createdAt DESC")
    List<Payment> findAllByBankId(@Param("bankId") String bankId);

}