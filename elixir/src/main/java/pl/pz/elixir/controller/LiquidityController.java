package pl.pz.elixir.controller;

import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.service.BankLiquidityService;

import java.util.Map;

@RestController
public class LiquidityController {

    private final BankLiquidityService bankLiquidityService;

    public LiquidityController(BankLiquidityService bankLiquidityService) {
        this.bankLiquidityService = bankLiquidityService;
    }

    @GetMapping("/api/elixir/liquidity")
    public Map<String, Double> balances() {
        return bankLiquidityService.getBalances();
    }

    @GetMapping("/api/elixir/blocked")
    public Map<String, Boolean> blocked() {
        return bankLiquidityService.getBlockedBanks();
    }

    @PostMapping("/api/elixir/block/{bank}")
    public String block(@PathVariable String bank) {

        bankLiquidityService.blockBank(bank);

        return "BANK BLOCKED: " + bank;
    }

    @PostMapping("/api/elixir/unblock/{bank}")
    public String unblock(@PathVariable String bank) {

        bankLiquidityService.unblockBank(bank);

        return "BANK UNBLOCKED: " + bank;
    }

    @PostMapping("/api/elixir/topup/{bank}")
    public String topup(@PathVariable String bank, @RequestParam Double amount) {

        bankLiquidityService.topUp(bank, amount);

        return "TOPUP SUCCESS: " + bank+ " +" + amount;
    }
}