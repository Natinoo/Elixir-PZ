package pl.pz.elixir.service;

import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.ElixirPaymentDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NettingService {

    public List<String> calculateNetting(List<ElixirPaymentDto> payments) {
        Map<String, Double> balanceMap = new HashMap<>();

        for (ElixirPaymentDto payment : payments) {
            String sender = payment.getSenderBankId();
            String receiver = payment.getReceiverBankId();
            Double amount = payment.getAmount();

            balanceMap.put(sender, balanceMap.getOrDefault(sender, 0.0) - amount);
            balanceMap.put(receiver, balanceMap.getOrDefault(receiver, 0.0) + amount);
        }

        List<Map.Entry<String, Double>> debtors = new ArrayList<>();
        List<Map.Entry<String, Double>> creditors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : balanceMap.entrySet()) {
            if (entry.getValue() < 0) {
                debtors.add(entry);
            } else if (entry.getValue() > 0) {
                creditors.add(entry);
            }
        }

        debtors.sort(Comparator.comparingDouble(Map.Entry::getValue));
        creditors.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<String> results = new ArrayList<>();

        int i = 0;
        int j = 0;

        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<String, Double> debtor = debtors.get(i);
            Map.Entry<String, Double> creditor = creditors.get(j);

            double debt = -debtor.getValue();
            double credit = creditor.getValue();
            double transfer = Math.min(debt, credit);

            results.add(debtor.getKey() + " pays " + creditor.getKey() + " = " + transfer);

            debtor.setValue(debtor.getValue() + transfer);
            creditor.setValue(creditor.getValue() - transfer);

            if (Math.abs(debtor.getValue()) < 0.01) {
                i++;
            }
            if (Math.abs(creditor.getValue()) < 0.01) {
                j++;
            }
        }

        return results;
    }
}