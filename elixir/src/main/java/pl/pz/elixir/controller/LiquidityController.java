package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.service.BankLiquidityService;
import pl.pz.elixir.service.SessionService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@Tag(name = "Zarządzanie płynnością banków", description = "Endpoints do monitorowania i zmiany stanów płynności oraz blokad banków")
public class LiquidityController {

    private final BankLiquidityService bankLiquidityService;
    private final SessionService sessionService;

    public LiquidityController(BankLiquidityService bankLiquidityService,
                               SessionService sessionService) {
        this.bankLiquidityService = bankLiquidityService;
        this.sessionService = sessionService;
    }

    @GetMapping(value = "/api/elixir/liquidity", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz bieżące salda banków", description = "Zwraca mapę: nazwa banku -> aktualne środki.")
    @ApiResponse(responseCode = "200", description = "Salda banków",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"BANK_A\": 10000.0, \"BANK_B\": 5000.0}")))
    public Map<String, BigDecimal> balances() {
        return bankLiquidityService.getBalances();
    }

    @GetMapping(value = "/api/elixir/blocked", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sprawdź, które banki są zablokowane", description = "Zwraca mapę: nazwa banku -> true/false.")
    @ApiResponse(responseCode = "200", description = "Status blokad",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"BANK_A\": false, \"BANK_B\": true}")))
    public Map<String, Boolean> blocked() {
        return bankLiquidityService.getBlockedBanks();
    }

    @PostMapping("/api/elixir/block/{bank}")
    @Operation(summary = "Zablokuj bank", description = "Bank zablokowany nie może wysyłać ani odbierać przelewów.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bank zablokowany",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "BANK BLOCKED: BANK_A"))),
            @ApiResponse(responseCode = "400", description = "Bank już jest zablokowany lub nieprawidłowa nazwa",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Bank already blocked"))),
            @ApiResponse(responseCode = "404", description = "Bank nie istnieje",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Bank not found")))
    })
    public String block(
            @Parameter(description = "Nazwa banku (np. BANK_A, BANK_B, BANK_C)", required = true, example = "BANK_B")
            @PathVariable String bank) {
        bankLiquidityService.blockBank(bank);
        return "BANK BLOCKED: " + bank;
    }

    @PostMapping("/api/elixir/unblock/{bank}")
    @Operation(summary = "Odblokuj bank", description = "Przywraca możliwość uczestnictwa w sesjach.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bank odblokowany",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "BANK UNBLOCKED: BANK_A"))),
            @ApiResponse(responseCode = "400", description = "Bank nie był zablokowany",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Bank was not blocked"))),
            @ApiResponse(responseCode = "404", description = "Bank nie istnieje",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Bank not found")))
    })
    public String unblock(
            @Parameter(description = "Nazwa banku", required = true, example = "BANK_B")
            @PathVariable String bank) {
        bankLiquidityService.unblockBank(bank);
        return "BANK UNBLOCKED: " + bank;
    }

    @PostMapping("/api/elixir/topup/{bank}")
    @Operation(summary = "Dodaj środki bankowi",
            description = "Zwiększa stan środków banku. Jeśli bank przekracza limit w bieżącej sesji, kwota top-up nie może przekroczyć minimalnej kwoty wymaganej do zejścia do dolnego limitu zadłużenia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Środki dodane",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "TOPUP SUCCESS: BANK_A +5000.00"))),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowa kwota, bank nie istnieje lub kwota przekracza wymagane zasilenie",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Kwota top-up przekracza wymagane zasilenie do limitu.")))
    })
    public String topup(
            @Parameter(description = "Nazwa banku", required = true, example = "BANK_A")
            @PathVariable String bank,
            @Parameter(description = "Kwota do dodania (dodatnia)", required = true, example = "5000.00")
            @RequestParam BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota top-up musi być większa od zera.");
        }

        SessionService.CurrentBankExposure exposure = sessionService.getCurrentBankExposure(bank);
        BigDecimal requiredTopUp = exposure.getRequiredTopUp() == null
                ? BigDecimal.ZERO
                : exposure.getRequiredTopUp();

        // Zgodnie z wymaganiem prowadzącego: bank może uzupełnić tylko tyle,
        // ile potrzeba, żeby zmieścić się w swoim dolnym limicie zadłużenia.
        // Gdy nie ma ryzyka płynności, zostawiamy techniczny top-up dostępny dla testów.
        if (requiredTopUp.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(requiredTopUp) > 0) {
            throw new IllegalArgumentException(
                    "Kwota top-up przekracza wymagane zasilenie do limitu. Maksymalnie: "
                            + requiredTopUp.toPlainString() + " PLN"
            );
        }

        bankLiquidityService.topUp(BankLiquidityService.ELIXIR, bank, amount);
        return "TOPUP SUCCESS: " + bank + " +" + amount.toPlainString()
                + " PLN" + (requiredTopUp.compareTo(BigDecimal.ZERO) > 0 ? " (uzupełnienie do dolnego limitu zadłużenia)" : "");
    }
}