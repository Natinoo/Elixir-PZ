package pl.pz.elixir.controller;

import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/elixir/history")
public class PaymentHistoryController {

    private final ElixirPaymentService paymentService;

    public PaymentHistoryController(ElixirPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<Payment> allPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/status/{status}")
    public List<Payment> byStatus(@PathVariable PaymentStatus status) {

        return paymentService.getPaymentsByStatus(status);
    }
}