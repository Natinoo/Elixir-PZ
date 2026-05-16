package pl.pz.elixir.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elixir/payments")
public class ElixirPaymentController {

    private final ElixirPaymentService paymentService;

    public ElixirPaymentController(ElixirPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestBody ElixirPaymentDto paymentDto) {

        return ResponseEntity.ok(
                paymentService.processPayment(paymentDto)
        );
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/queued")
    public List<Payment> queuedPayments() {

        return paymentService.getPaymentsByStatus(
                PaymentStatus.QUEUED
        );
    }

    @GetMapping("/processed")
    public List<Payment> processedPayments() {

        return paymentService.getPaymentsByStatus(
                PaymentStatus.PROCESSED
        );
    }

    @GetMapping("/blocked")
    public List<Payment> blockedPayments() {

        return paymentService.getPaymentsByStatus(
                PaymentStatus.BLOCKED
        );
    }

    @GetMapping("/rejected")
    public List<Payment> rejectedPayments() {

        return paymentService.getPaymentsByStatus(
                PaymentStatus.REJECTED
        );
    }
}