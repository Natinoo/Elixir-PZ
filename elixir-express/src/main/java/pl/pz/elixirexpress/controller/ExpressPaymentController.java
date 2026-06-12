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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixirexpress.dto.ExpressPaymentDto;
import pl.pz.elixirexpress.model.BankAccount;
import pl.pz.elixirexpress.model.Payment;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.service.ExpressPaymentService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/express")
@Tag(name = "Elixir Express", description = "Natychmiastowe przelewy Express, płynność banków i historia transakcji")
public class ExpressPaymentController {

    private final ExpressPaymentService expressPaymentService;

    public ExpressPaymentController(ExpressPaymentService expressPaymentService) {
        this.expressPaymentService = expressPaymentService;
    }

    @PostMapping(value = "/payments", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Utwórz nowy przelew express",
            description = "Tworzy natychmiastowy przelew Express. Express sam loguje i lokalnie rozlicza transakcję. Jeśli bank nadawcy przekroczyłby limit zadłużenia, przelew trafia do GRIDLOCK_HELD i wysyłany jest wyłącznie request płynnościowy do Sorbnetu.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Przelew przyjęty albo zatrzymany przez płynność",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            examples = @ExampleObject(value = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><PaymentResponse><paymentId>EXP-1</paymentId><status>PROCESSED</status><channel>EXPRESS</channel><message>Przelew express rozliczony lokalnie w systemie Express.</message></PaymentResponse>"))),
            @ApiResponse(responseCode = "400", description = "Błędne dane wejściowe")
    })
    public ResponseEntity<String> createPayment(@RequestBody ExpressPaymentDto paymentDto) {
        try {
            Map<String, Object> result = expressPaymentService.processPayment(paymentDto);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(responseXml(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(errorXml(e.getMessage()));
        }
    }

    @GetMapping(value = "/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz wszystkie przelewy Express")
    public List<Payment> getAllPayments() {
        return expressPaymentService.getAllPayments();
    }

    @GetMapping(value = "/payments/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelew po ID")
    public ResponseEntity<?> getPaymentById(@PathVariable String paymentId) {
        Payment payment = expressPaymentService.getPaymentById(paymentId);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Payment not found", "paymentId", paymentId));
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping(value = "/payments/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy według statusu")
    public ResponseEntity<?> getPaymentsByStatus(
            @Parameter(description = "Status przelewu", required = true,
                    schema = @Schema(allowableValues = {"QUEUED", "PROCESSED", "GRIDLOCK_HELD", "BLOCKED", "REJECTED"}))
            @PathVariable String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(expressPaymentService.getPaymentsByStatus(paymentStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
        }
    }

    @PostMapping(value = "/payments/{paymentId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Anuluj przelew", description = "Anuluje przelew, jeśli nie został jeszcze przetworzony.")
    public ResponseEntity<?> cancelPayment(@PathVariable String paymentId) {
        Payment payment = expressPaymentService.getPaymentById(paymentId);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Payment not found", "paymentId", paymentId));
        }
        boolean cancelled = expressPaymentService.cancelPayment(paymentId);
        if (!cancelled) {
            return ResponseEntity.badRequest().body(Map.of("error", "Payment cannot be cancelled", "paymentId", paymentId));
        }
        return ResponseEntity.ok(Map.of("paymentId", paymentId, "status", "REJECTED"));
    }

    @PostMapping(value = "/payments/{paymentId}/retry", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ponów przelew zatrzymany przez gridlock")
    public ResponseEntity<?> retryPayment(@PathVariable String paymentId) {
        boolean retried = expressPaymentService.retryHeldPayment(paymentId);
        if (!retried) {
            return ResponseEntity.badRequest().body(Map.of("paymentId", paymentId, "retried", false));
        }
        return ResponseEntity.ok(Map.of("paymentId", paymentId, "retried", true));
    }

    @GetMapping(value = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz konta techniczne banków Express")
    public List<BankAccount> getAccounts() {
        return expressPaymentService.getAllAccounts();
    }

    @GetMapping(value = "/accounts/{bankId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz konto techniczne banku Express")
    public BankAccount getAccount(@PathVariable String bankId) {
        return expressPaymentService.getAccount(bankId);
    }

    @PostMapping(value = "/accounts/{bankId}/topup", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Zasil konto techniczne banku Express")
    public BankAccount topUpAccount(@PathVariable String bankId, @RequestParam BigDecimal amount) {
        return expressPaymentService.topUpBank(bankId, amount);
    }

    @PostMapping(value = "/accounts/{bankId}/unblock", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Odblokuj bank Express")
    public BankAccount unblockAccount(@PathVariable String bankId) {
        return expressPaymentService.unblockBank(bankId);
    }

    private String responseXml(Map<String, Object> result) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<PaymentResponse>" +
                "<paymentId>" + escapeXml(String.valueOf(result.get("paymentId"))) + "</paymentId>" +
                "<status>" + escapeXml(String.valueOf(result.get("status"))) + "</status>" +
                "<channel>" + escapeXml(String.valueOf(result.get("channel"))) + "</channel>" +
                "<message>" + escapeXml(String.valueOf(result.get("message"))) + "</message>" +
                "</PaymentResponse>";
    }

    private String errorXml(String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><message>" + escapeXml(message) + "</message></Error>";
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}