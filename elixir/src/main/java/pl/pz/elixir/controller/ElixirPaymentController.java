package pl.pz.elixir.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.service.ElixirPaymentService;

import java.util.Map;

@RestController
@RequestMapping("/api/elixir/payments")
public class ElixirPaymentController {

    private final ElixirPaymentService paymentService;

    public ElixirPaymentController(ElixirPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody ElixirPaymentDto paymentDto) {
        return ResponseEntity.ok(paymentService.processPayment(paymentDto));
    }
}