package pl.pz.sorbnet.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.sorbnet.dto.SorbnetPaymentDto;
import pl.pz.sorbnet.model.Payment;
import pl.pz.sorbnet.repository.PaymentRepository;
import pl.pz.sorbnet.service.SorbnetPaymentService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sorbnet/payments")
public class SorbnetPaymentController {

    private final SorbnetPaymentService service;
    private final PaymentRepository paymentRepo;

    public SorbnetPaymentController(SorbnetPaymentService service,
                                     PaymentRepository paymentRepo) {
        this.service = service;
        this.paymentRepo = paymentRepo;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> send(@RequestBody SorbnetPaymentDto dto) {
        return ResponseEntity.ok(service.process(dto));
    }

    @GetMapping
    public List<Payment> history(
            @RequestParam String bankId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {

        LocalDateTime maxFrom = LocalDateTime.now().minusMonths(1);
        LocalDateTime start = from == null
                ? LocalDate.now().atStartOfDay()
                : from.atStartOfDay();
        if (start.isBefore(maxFrom)) start = maxFrom;

        return paymentRepo.findByBankIdAndFrom(bankId, start);
    }

    @GetMapping("/{paymentId}")
    public Payment getById(@PathVariable String paymentId) {
        return paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono: " + paymentId));
    }
}