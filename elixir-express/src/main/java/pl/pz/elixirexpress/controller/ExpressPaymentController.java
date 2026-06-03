package pl.pz.elixirexpress.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/express/payments")
@Tag(name = "Przelewy Express", description = "Zarządzanie szybkimi przelewami Elixir Express")
public class ExpressPaymentController {

    private final ExpressPaymentService expressPaymentService;

    public ExpressPaymentController(ExpressPaymentService expressPaymentService) {
        this.expressPaymentService = expressPaymentService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Utwórz nowy przelew express", 
               description = "Tworzy nowy przelew w systemie Elixir Express. Przelew otrzymuje status QUEUED i jest natychmiast przesyłany do SORBNET.")
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
    public ResponseEntity<?> createPayment(@RequestBody ExpressPaymentDto paymentDto) {
        try {
            return ResponseEntity.ok(expressPaymentService.processPayment(paymentDto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz wszystkie przelewy", description = "Zwraca listę wszystkich przelewów.")
    @ApiResponse(responseCode = "200", description = "Lista przelewów",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(expressPaymentService.getAllPayments());
    }

    @GetMapping(value = "/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelew po ID", description = "Zwraca szczegóły przelewu o podanym identyfikatorze.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Przelew znaleziony"),
        @ApiResponse(responseCode = "404", description = "Przelew nie znaleziony",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"error\":\"Payment not found\",\"paymentId\":\"123\"}")))
    })
    public ResponseEntity<?> getPaymentById(@Parameter(description = "Identyfikator przelewu", required = true) 
                                            @PathVariable String paymentId) {
        Payment payment = expressPaymentService.getPaymentById(paymentId);
        if (payment == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Payment not found", "paymentId", paymentId));
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping(value = "/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy według statusu", 
               description = "Zwraca listę przelewów o podanym statusie (QUEUED, PROCESSED, BLOCKED, REJECTED).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista przelewów"),
        @ApiResponse(responseCode = "400", description = "Nieprawidłowy status",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"error\":\"Invalid status: INVALID\"}")))
    })
    public ResponseEntity<?> getPaymentsByStatus(@Parameter(description = "Status przelewu", required = true, 
                                                           schema = @Schema(allowableValues = {"QUEUED", "PROCESSED", "BLOCKED", "REJECTED"}))
                                                @PathVariable String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(expressPaymentService.getPaymentsByStatus(paymentStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
        }
    }

    @PostMapping(value = "/{paymentId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Anuluj przelew", description = "Anuluje przelew, jeśli nie został jeszcze przetworzony.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Przelew anulowany",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"paymentId\":\"123\",\"status\":\"CANCELLED\"}"))),
        @ApiResponse(responseCode = "400", description = "Nie można anulować przelewu (już przetworzony)",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"error\":\"Payment cannot be cancelled\",\"paymentId\":\"123\",\"status\":\"PROCESSED\"}"))),
        @ApiResponse(responseCode = "404", description = "Przelew nie znaleziony")
    })
    public ResponseEntity<?> cancelPayment(@Parameter(description = "Identyfikator przelewu", required = true)
                                           @PathVariable String paymentId) {
        Payment payment = expressPaymentService.getPaymentById(paymentId);
        if (payment == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Payment not found", "paymentId", paymentId));
        }
        boolean cancelled = expressPaymentService.cancelPayment(paymentId);
        if (!cancelled) {
            return ResponseEntity.badRequest().body(Map.of("error", "Payment cannot be cancelled", 
                    "paymentId", paymentId, "status", payment.getStatus().name()));
        }
        return ResponseEntity.ok(Map.of("paymentId", paymentId, "status", "CANCELLED"));
    }
}