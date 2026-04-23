package pl.pz.elixirexpress.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixirexpress.dto.ExpressPaymentDto;
import pl.pz.elixirexpress.service.ExpressPaymentService;

import java.util.Map;

@RestController
@RequestMapping("/api/express/payments")
public class ExpressPaymentController {

    private final ExpressPaymentService service;

    public ExpressPaymentController(ExpressPaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> send(@RequestBody ExpressPaymentDto dto) {
        return ResponseEntity.ok(service.process(dto));
    }
}