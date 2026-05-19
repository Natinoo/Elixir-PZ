package pl.pz.elixir.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elixir/payments")
public class ElixirPaymentController {

    private final ElixirPaymentService paymentService;

    public ElixirPaymentController(ElixirPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestBody ElixirPaymentDto paymentDto
    ) {
        return ResponseEntity.ok(
                paymentService.processPayment(paymentDto)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(IllegalArgumentException ex) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping(value = "/queued", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Payment> queuedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.QUEUED);
    }

    @GetMapping(value = "/processed", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Payment> processedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.PROCESSED);
    }

    @GetMapping(value = "/blocked", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Payment> blockedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.BLOCKED);
    }

    @GetMapping(value = "/rejected", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Payment> rejectedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.REJECTED);
    }
}