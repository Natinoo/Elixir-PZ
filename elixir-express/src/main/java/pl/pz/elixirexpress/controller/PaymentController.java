package pl.pz.elixirexpress.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixirexpress.dto.ExpressPaymentDto;
import pl.pz.elixirexpress.model.Payment;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.service.ExpressPaymentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final ExpressPaymentService expressPaymentService;

    public PaymentController(ExpressPaymentService expressPaymentService) {
        this.expressPaymentService = expressPaymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestBody ExpressPaymentDto paymentDto) {

        return ResponseEntity.ok(
                expressPaymentService.processPayment(paymentDto)
        );
    }

    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments() {

        return ResponseEntity.ok(
                expressPaymentService.getAllPayments()
        );
    }

    @GetMapping("/payments/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(
            @PathVariable String status) {

        try {
            PaymentStatus paymentStatus =
                    PaymentStatus.valueOf(status.toUpperCase());

            return ResponseEntity.ok(
                    expressPaymentService.getPaymentsByStatus(paymentStatus)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}