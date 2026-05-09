package pl.pz.elixir.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BankLiquidityService {

    // limity zadłużenia
    private final Map<String, Double> debtLimits = new HashMap<>();

    // aktualne salda
    private final Map<String, Double> balances = new HashMap<>();

    // blokady
    private final Map<String, Boolean> blockedBanks = new HashMap<>();

    public BankLiquidityService() {

        // przykładowe banki
        debtLimits.put("BANK_A", -1000.0);
        debtLimits.put("BANK_B", -1000.0);
        debtLimits.put("BANK_C", -1000.0);

        balances.put("BANK_A", 0.0);
        balances.put("BANK_B", 0.0);
        balances.put("BANK_C", 0.0);

        blockedBanks.put("BANK_A", false);
        blockedBanks.put("BANK_B", false);
        blockedBanks.put("BANK_C", false);
    }

    public boolean isBlocked(String bank) {
        return blockedBanks.getOrDefault(bank, false);
    }

    public void applyTransaction(String sender,
                                 String receiver,
                                 Double amount) {

        balances.put(sender,
                balances.getOrDefault(sender, 0.0) - amount);

        balances.put(receiver,
                balances.getOrDefault(receiver, 0.0) + amount);

        checkLimit(sender);
    }

    private void checkLimit(String bank) {

        double balance = balances.getOrDefault(bank, 0.0);
        double limit = debtLimits.getOrDefault(bank, -1000.0);

        if (balance < limit) {

            blockedBanks.put(bank, true);

            System.out.println(
                    "🚨 BANK BLOCKED: "
                            + bank
                            + " balance="
                            + balance
            );
        }
    }

    public Map<String, Double> getBalances() {
        return balances;
    }

    public Map<String, Boolean> getBlockedBanks() {
        return blockedBanks;
    }
    public void blockBank(String bank) {
        blockedBanks.put(bank, true);
    }

    public void unblockBank(String bank) {
        blockedBanks.put(bank, false);
    }
}