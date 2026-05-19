package pl.pz.elixirexpress;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixirexpress.dto.ExpressPaymentDto;
import pl.pz.elixirexpress.model.Payment;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.service.ExpressPaymentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/express/payments")
public class ExpressPaymentController {

    private final ExpressPaymentService expressPaymentService;

    public ExpressPaymentController(ExpressPaymentService expressPaymentService) {
        this.expressPaymentService = expressPaymentService;
    }

    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody ExpressPaymentDto paymentDto) {
        try {
            return ResponseEntity.ok(expressPaymentService.processPayment(paymentDto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(expressPaymentService.getAllPayments());
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPaymentById(@PathVariable String paymentId) {
        Payment payment = expressPaymentService.getPaymentById(paymentId);
        if (payment == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Payment not found",
                    "paymentId", paymentId
            ));
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getPaymentsByStatus(@PathVariable String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(expressPaymentService.getPaymentsByStatus(paymentStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid status: " + status
            ));
        }
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<?> cancelPayment(@PathVariable String paymentId) {
        Payment payment = expressPaymentService.getPaymentById(paymentId);
        if (payment == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Payment not found",
                    "paymentId", paymentId
            ));
        }

        boolean cancelled = expressPaymentService.cancelPayment(paymentId);
        if (!cancelled) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Payment cannot be cancelled",
                    "paymentId", paymentId,
                    "status", payment.getStatus().name()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "paymentId", paymentId,
                "status", "CANCELLED"
        ));
    }
}