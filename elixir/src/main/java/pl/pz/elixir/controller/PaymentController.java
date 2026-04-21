package pl.pz.elixir.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.dto.PaymentDto;
import pl.pz.elixir.service.PaymentService;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody PaymentDto paymentDto) {
        return ResponseEntity.ok(paymentService.processPayment(paymentDto));
    }
}