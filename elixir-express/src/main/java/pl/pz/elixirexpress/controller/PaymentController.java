package pl.pz.elixir;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;
import pl.pz.elixir.service.SessionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final ElixirPaymentService elixirPaymentService;
    private final SessionService sessionService;

    public PaymentController(ElixirPaymentService elixirPaymentService,
                             SessionService sessionService) {
        this.elixirPaymentService = elixirPaymentService;
        this.sessionService = sessionService;
    }

    @PostMapping("/payments")
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody ElixirPaymentDto paymentDto) {
        return ResponseEntity.ok(elixirPaymentService.processPayment(paymentDto));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(elixirPaymentService.getAllPayments());
    }

    @GetMapping("/payments/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(elixirPaymentService.getPaymentsByStatus(paymentStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/session")
    public ResponseEntity<?> getCurrentSession() {
        return ResponseEntity.ok(sessionService.getCurrentSession());
    }

    @PostMapping("/session/close/{name}")
    public ResponseEntity<Map<String, String>> closeSession(@PathVariable String name) {
        sessionService.closeSession(name.toUpperCase());
        return ResponseEntity.ok(Map.of(
                "status", "SESSION_CLOSED",
                "session", name.toUpperCase()
        ));
    }
}