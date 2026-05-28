package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/elixir/history")
@Tag(name = "Historia przelewów", description = "Endpoints do przeglądania historii przelewów (wszystkie sesje)")
public class PaymentHistoryController {

    private final ElixirPaymentService paymentService;

    public PaymentHistoryController(ElixirPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Wszystkie przelewy (historia)", description = "Zwraca wszystkie przelewy ze wszystkich sesji.")
    @ApiResponse(responseCode = "200", description = "Lista przelewów",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "[{\"paymentId\":\"abc123\",\"status\":\"QUEUED\"}]")))
    public List<Payment> allPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        return payments != null ? payments : List.of();
    }

    @GetMapping(value = "/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Przelewy według statusu", description = "Filtruje przelewy po statusie (QUEUED, PROCESSED, BLOCKED, REJECTED).")
    @ApiResponse(responseCode = "200", description = "Lista przelewów",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "[{\"paymentId\":\"def456\",\"status\":\"PROCESSED\"}]")))
    public List<Payment> byStatus(
            @Parameter(description = "Status przelewu", required = true, schema = @Schema(implementation = PaymentStatus.class))
            @PathVariable PaymentStatus status) {
        List<Payment> payments = paymentService.getPaymentsByStatus(status);
        return payments != null ? payments : List.of();
    }
}