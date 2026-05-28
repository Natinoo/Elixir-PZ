package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/elixir/payments")
@Tag(name = "Przelewy Elixir", description = "Zarządzanie przelewami w systemie Elixir (wysyłanie, lista, filtrowanie)")
public class ElixirPaymentController {

    private static final Logger log = LoggerFactory.getLogger(ElixirPaymentController.class);
    private final ElixirPaymentService paymentService;

    public ElixirPaymentController(ElixirPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Utwórz nowy przelew", description = "Przyjmuje przelew w formacie XML i dodaje go do bieżącej sesji. Przelew otrzymuje status QUEUED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Przelew przyjęty do sesji",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            examples = @ExampleObject(value = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<PaymentResponse>\n    <paymentId>123e4567-e89b-12d3-a456-426614174000</paymentId>\n    <status>QUEUED_FOR_SESSION</status>\n</PaymentResponse>"))),
            @ApiResponse(responseCode = "400", description = "Błędne dane",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            examples = @ExampleObject(value = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Error>\n    <message>Amount must be greater than 0</message>\n</Error>")))
    })
    public ResponseEntity<String> createPayment(@RequestBody ElixirPaymentDto paymentDto) {
        log.info("=== POST /api/elixir/payments ===");
        log.info("Received DTO: sender={}, receiver={}, amount={}, title={}",
                paymentDto.getSenderAccount(), paymentDto.getReceiverAccount(),
                paymentDto.getAmount(), paymentDto.getTitle());
        try {
            String responseXml = paymentService.processPayment(paymentDto);
            log.info("Response sent successfully");
            return ResponseEntity.ok(responseXml);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            String errorXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><message>" + e.getMessage() + "</message></Error>";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorXml);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            String errorXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><message>Internal server error: " + e.getMessage() + "</message></Error>";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorXml);
        }
    }

    // Usuń lub zakomentuj stary @ExceptionHandler dla IllegalArgumentException
    // Jeśli istnieje, usuń go całkowicie.

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz wszystkie przelewy")
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping(value = "/queued", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy w kolejce")
    public List<Payment> queuedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.QUEUED);
    }

    @GetMapping(value = "/processed", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy przetworzone")
    public List<Payment> processedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.PROCESSED);
    }

    @GetMapping(value = "/blocked", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy zablokowane")
    public List<Payment> blockedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.BLOCKED);
    }

    @GetMapping(value = "/rejected", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy odrzucone")
    public List<Payment> rejectedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.REJECTED);
    }
}