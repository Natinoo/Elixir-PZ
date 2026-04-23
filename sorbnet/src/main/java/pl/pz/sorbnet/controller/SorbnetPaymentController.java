package pl.pz.sorbnet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.sorbnet.service.SorbnetPaymentService;
import pl.pz.sorbnet.dto.SorbnetPaymentDto;

import java.util.Map;

@RestController
@RequestMapping("/api/sorbnet/payments")
public class SorbnetPaymentController {

    private final SorbnetPaymentService service;

    public SorbnetPaymentController(SorbnetPaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> send(@RequestBody SorbnetPaymentDto dto) {
        return ResponseEntity.ok(service.process(dto));
    }
}