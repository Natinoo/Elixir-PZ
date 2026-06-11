package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.service.BankLiquidityService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@Tag(name = "Zarządzanie płynnością banków", description = "Monitoring kont banków per serwis: ELIXIR, SORBNET, później EXPRESS")
public class LiquidityController {

    private final BankLiquidityService bankLiquidityService;

    public LiquidityController(BankLiquidityService bankLiquidityService) {
        this.bankLiquidityService = bankLiquidityService;
    }

    @GetMapping(value = "/api/elixir/liquidity", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz salda banków dla ELIXIR")
    public Map<String, BigDecimal> elixirBalances() {
        return bankLiquidityService.getBalances(BankLiquidityService.ELIXIR);
    }

    @GetMapping(value = "/api/liquidity/{serviceCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz salda banków dla wskazanego serwisu")
    public Map<String, BigDecimal> balances(
            @Parameter(example = "ELIXIR") @PathVariable String serviceCode) {
        return bankLiquidityService.getBalances(serviceCode);
    }

    @GetMapping(value = "/api/elixir/blocked", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sprawdź blokady banków dla ELIXIR")
    public Map<String, Boolean> elixirBlocked() {
        return bankLiquidityService.getBlockedBanks(BankLiquidityService.ELIXIR);
    }

    @GetMapping(value = "/api/liquidity/{serviceCode}/blocked", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sprawdź blokady banków dla wskazanego serwisu")
    public Map<String, Boolean> blocked(
            @Parameter(example = "ELIXIR") @PathVariable String serviceCode) {
        return bankLiquidityService.getBlockedBanks(serviceCode);
    }

    @PostMapping("/api/elixir/block/{bank}")
    @Operation(summary = "Zablokuj bank w ELIXIR")
    public String blockElixir(@PathVariable String bank) {
        bankLiquidityService.blockBank(BankLiquidityService.ELIXIR, bank);
        return "BANK BLOCKED IN ELIXIR: " + bank;
    }

    @PostMapping("/api/liquidity/{serviceCode}/block/{bank}")
    @Operation(summary = "Zablokuj bank w wybranym serwisie")
    public String block(@PathVariable String serviceCode, @PathVariable String bank) {
        bankLiquidityService.blockBank(serviceCode, bank);
        return "BANK BLOCKED IN " + serviceCode + ": " + bank;
    }

    @PostMapping("/api/elixir/unblock/{bank}")
    @Operation(summary = "Odblokuj bank w ELIXIR")
    public String unblockElixir(@PathVariable String bank) {
        bankLiquidityService.unblockBank(BankLiquidityService.ELIXIR, bank);
        return "BANK UNBLOCKED IN ELIXIR: " + bank;
    }

    @PostMapping("/api/liquidity/{serviceCode}/unblock/{bank}")
    @Operation(summary = "Odblokuj bank w wybranym serwisie")
    public String unblock(@PathVariable String serviceCode, @PathVariable String bank) {
        bankLiquidityService.unblockBank(serviceCode, bank);
        return "BANK UNBLOCKED IN " + serviceCode + ": " + bank;
    }

    @PostMapping("/api/elixir/topup/{bank}")
    @Operation(summary = "Dodaj środki bankowi w ELIXIR")
    public String topupElixir(@PathVariable String bank, @RequestParam BigDecimal amount) {
        bankLiquidityService.topUp(BankLiquidityService.ELIXIR, bank, amount);
        return "TOPUP SUCCESS IN ELIXIR: " + bank + " +" + amount;
    }

    @PostMapping("/api/liquidity/{serviceCode}/topup/{bank}")
    @Operation(summary = "Dodaj środki bankowi w wybranym serwisie")
    public String topup(@PathVariable String serviceCode, @PathVariable String bank, @RequestParam BigDecimal amount) {
        bankLiquidityService.topUp(serviceCode, bank, amount);
        return "TOPUP SUCCESS IN " + serviceCode + ": " + bank + " +" + amount;
    }

    @PostMapping("/api/liquidity/transfer")
    @Operation(summary = "Ręczny transfer płynności między serwisami dla jednego banku")
    public String transferBetweenServices(@RequestParam String sourceServiceCode,
                                          @RequestParam String targetServiceCode,
                                          @RequestParam String bank,
                                          @RequestParam BigDecimal amount) {
        bankLiquidityService.transferBetweenServices(sourceServiceCode, targetServiceCode, bank, amount);
        return "LIQUIDITY TRANSFER SUCCESS: " + bank + " " + sourceServiceCode + " -> " + targetServiceCode + " +" + amount;
    }
}