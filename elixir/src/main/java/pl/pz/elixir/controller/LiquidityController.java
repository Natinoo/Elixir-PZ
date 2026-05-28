package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.service.BankLiquidityService;

import java.util.Map;

@RestController
@Tag(name = "Zarządzanie płynnością banków", description = "Endpoints do monitorowania i zmiany stanów płynności oraz blokad banków")
public class LiquidityController {

    private final BankLiquidityService bankLiquidityService;

    public LiquidityController(BankLiquidityService bankLiquidityService) {
        this.bankLiquidityService = bankLiquidityService;
    }

    @GetMapping(value = "/api/elixir/liquidity", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz bieżące salda banków", description = "Zwraca mapę: nazwa banku -> aktualne środki.")
    @ApiResponse(responseCode = "200", description = "Salda banków",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"BANK_A\": 10000.0, \"BANK_B\": 5000.0}")))
    public Map<String, Double> balances() {
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
    @Operation(summary = "Dodaj środki bankowi", description = "Zwiększa stan środków banku o podaną kwotę.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Środki dodane",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "TOPUP SUCCESS: BANK_A +5000.0"))),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowa kwota (<=0) lub bank nie istnieje",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Amount must be positive")))
    })
    public String topup(
            @Parameter(description = "Nazwa banku", required = true, example = "BANK_A")
            @PathVariable String bank,
            @Parameter(description = "Kwota do dodania (dodatnia)", required = true, example = "5000.00")
            @RequestParam Double amount) {
        bankLiquidityService.topUp(bank, amount);
        return "TOPUP SUCCESS: " + bank + " +" + amount;
    }
}