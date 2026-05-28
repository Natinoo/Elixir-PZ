package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elixir/payments")
@Tag(name = "Przelewy Elixir", description = "Zarządzanie przelewami w systemie Elixir (wysyłanie, lista, filtrowanie)")
public class ElixirPaymentController {

    private final ElixirPaymentService paymentService;

    public ElixirPaymentController(ElixirPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    @Operation(summary = "Utwórz nowy przelew", description = "Przyjmuje przelew w formacie XML i dodaje go do bieżącej sesji. Przelew otrzymuje status QUEUED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Przelew przyjęty do sesji",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            examples = @ExampleObject(value = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<PaymentResponse>\n    <paymentId>123e4567-e89b-12d3-a456-426614174000</paymentId>\n    <status>QUEUED</status>\n    <channel>ELIXIR</channel>\n</PaymentResponse>"))),
            @ApiResponse(responseCode = "400", description = "Błędne dane (np. ujemna kwota, brak tytułu)",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            examples = @ExampleObject(value = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Error>\n    <message>Amount must be greater than 0</message>\n</Error>")))
    })
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody ElixirPaymentDto paymentDto) {
        return ResponseEntity.ok(paymentService.processPayment(paymentDto));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(IllegalArgumentException ex) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz wszystkie przelewy", description = "Zwraca listę wszystkich przelewów (wszystkich sesji).")
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping(value = "/queued", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy w kolejce", description = "Zwraca przelewy o statusie QUEUED.")
    @ApiResponse(responseCode = "200", description = "Lista przelewów QUEUED",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "[{\"paymentId\":\"a1b2c3\",\"status\":\"QUEUED\",\"amount\":100.0,\"currency\":\"PLN\"}]")))
    public List<Payment> queuedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.QUEUED);
    }

    @GetMapping(value = "/processed", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy przetworzone", description = "Zwraca przelewy o statusie PROCESSED.")
    @ApiResponse(responseCode = "200", description = "Lista przelewów PROCESSED",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "[{\"paymentId\":\"d4e5f6\",\"status\":\"PROCESSED\",\"amount\":250.0,\"currency\":\"PLN\"}]")))
    public List<Payment> processedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.PROCESSED);
    }

    @GetMapping(value = "/blocked", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy zablokowane", description = "Zwraca przelewy o statusie BLOCKED (z powodu braku płynności).")
    @ApiResponse(responseCode = "200", description = "Lista przelewów BLOCKED",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "[{\"paymentId\":\"g7h8i9\",\"status\":\"BLOCKED\",\"amount\":500.0,\"currency\":\"PLN\"}]")))
    public List<Payment> blockedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.BLOCKED);
    }

    @GetMapping(value = "/rejected", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy odrzucone", description = "Zwraca przelewy o statusie REJECTED.")
    @ApiResponse(responseCode = "200", description = "Lista przelewów REJECTED",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "[{\"paymentId\":\"j0k1l2\",\"status\":\"REJECTED\",\"amount\":30.0,\"currency\":\"PLN\"}]")))
    public List<Payment> rejectedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.REJECTED);
    }
}