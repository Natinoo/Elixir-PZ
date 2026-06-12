package pl.pz.elixirexpress.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixirexpress.dto.ExpressPaymentDto;
import pl.pz.elixirexpress.model.Payment;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.service.ExpressPaymentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Alternatywne API przelewów", description = "Alternatywne endpointy do zarządzania przelewami Express bez przedrostka /express")
public class PaymentController {

    private final ExpressPaymentService expressPaymentService;

    public PaymentController(ExpressPaymentService expressPaymentService) {
        this.expressPaymentService = expressPaymentService;
    }

    @PostMapping("/payments")
    @Operation(summary = "Utwórz nowy przelew express alternatywnym endpointem")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Przelew obsłużony",
                    content = @Content(examples = @ExampleObject(value = "{\"paymentId\":\"EXP-1\",\"status\":\"QUEUED\",\"channel\":\"EXPRESS\"}"))),
            @ApiResponse(responseCode = "400", description = "Błędne dane wejściowe")
    })
    public ResponseEntity<?> createPayment(@RequestBody ExpressPaymentDto paymentDto) {
        try {
            return ResponseEntity.ok(expressPaymentService.processPayment(paymentDto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/payments")
    @Operation(summary = "Pobierz wszystkie przelewy Express")
    public List<Payment> getAllPayments() {
        return expressPaymentService.getAllPayments();
    }

    @GetMapping("/payments/status/{status}")
    @Operation(summary = "Pobierz przelewy według statusu")
    public ResponseEntity<?> getPaymentsByStatus(@PathVariable String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(expressPaymentService.getPaymentsByStatus(paymentStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
        }
    }
}