package com.example.elixir.controller;

import com.example.elixir.dto.PaymentDto;
import com.example.elixir.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<String> createPayment(@RequestBody PaymentDto paymentDto) {
        paymentService.processPayment(paymentDto);
        return ResponseEntity.ok("Payment received");
    }
}