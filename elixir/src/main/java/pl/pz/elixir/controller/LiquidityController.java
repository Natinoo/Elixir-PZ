package pl.pz.elixir.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
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
}