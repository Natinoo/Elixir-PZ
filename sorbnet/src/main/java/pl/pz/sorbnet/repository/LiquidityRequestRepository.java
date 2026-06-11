package pl.pz.sorbnet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pz.sorbnet.model.LiquidityRequest;
import pl.pz.sorbnet.model.LiquidityRequestStatus;

import java.util.List;

public interface LiquidityRequestRepository extends JpaRepository<LiquidityRequest, String> {

    List<LiquidityRequest> findByStatusOrderByReceivedAtAsc(LiquidityRequestStatus status);

    List<LiquidityRequest> findByBankIdOrderByReceivedAtDesc(String bankId);
}