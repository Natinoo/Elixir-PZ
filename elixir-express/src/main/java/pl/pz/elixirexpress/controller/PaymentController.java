package pl.pz.elixirexpress.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixirexpress.dto.ExpressPaymentDto;
import pl.pz.elixirexpress.model.Payment;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.service.ExpressPaymentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Alternatywne API przelewów", description = "Alternatywne endpointy do zarządzania przelewami Express (bez przedrostka /express)")
public class PaymentController {

    private final ExpressPaymentService expressPaymentService;

    public PaymentController(ExpressPaymentService expressPaymentService) {
        this.expressPaymentService = expressPaymentService;
    }

    @PostMapping(value = "/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Utwórz nowy przelew express (alternatywny)",
               description = "Tworzy nowy przelew w systemie Elixir Express (alias dla /api/express/payments).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Przelew utworzony pomyślnie",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"paymentId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"QUEUED\",\"channel\":\"EXPRESS\"}"))),
        @ApiResponse(responseCode = "400", description = "Błędne dane wejściowe",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"error\":\"Amount must be greater than 0\"}"))),
        @ApiResponse(responseCode = "503", description = "System niedostępny (gridlock/emergency)",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"error\":\"System temporarily unavailable due to gridlock. Try again later.\"}")))
    })
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody ExpressPaymentDto paymentDto) {
        return ResponseEntity.ok(expressPaymentService.processPayment(paymentDto));
    }

    @GetMapping(value = "/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz wszystkie przelewy (alternatywny)",
               description = "Zwraca listę wszystkich przelewów (alias dla /api/express/payments).")
    @ApiResponse(responseCode = "200", description = "Lista przelewów",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(expressPaymentService.getAllPayments());
    }

    @GetMapping(value = "/payments/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy według statusu (alternatywny)",
               description = "Zwraca listę przelewów o podanym statusie (alias dla /api/express/payments/status/{status}).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista przelewów"),
        @ApiResponse(responseCode = "400", description = "Nieprawidłowy status",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "")))
    })
    public ResponseEntity<List<Payment>> getPaymentsByStatus(
            @Parameter(description = "Status przelewu", required = true,
                      schema = @Schema(allowableValues = {"QUEUED", "PROCESSED", "BLOCKED", "REJECTED"}))
            @PathVariable String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(expressPaymentService.getPaymentsByStatus(paymentStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}