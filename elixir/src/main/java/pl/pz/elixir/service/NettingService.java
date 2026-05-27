package pl.pz.elixir.service;

import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.ElixirPaymentDto;


import java.util.*;

@Service
public class NettingService {

    public List<String> calculateNetting(List<ElixirPaymentDto> payments) {

        Map<String, Double> balanceMap = new HashMap<>();

        // Liczenie sald
        for (ElixirPaymentDto p : payments) {

            String sender = p.getSenderAccount();
            String receiver = p.getReceiverAccount();
            Double amount = p.getAmount();

            balanceMap.put(sender, balanceMap.getOrDefault(sender, 0.0) - amount);
            balanceMap.put(receiver, balanceMap.getOrDefault(receiver, 0.0) + amount);
        }

        // Podział na dłużników i wierzycieli
        List<Map.Entry<String, Double>> debtors = new ArrayList<>();
        List<Map.Entry<String, Double>> creditors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : balanceMap.entrySet()) {
            if (entry.getValue() < 0) {
                debtors.add(entry);
            } else if (entry.getValue() > 0) {
                creditors.add(entry);
            }
        }

        // Sortowanie
        debtors.sort(Comparator.comparingDouble(Map.Entry::getValue));
        creditors.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Netting
        List<String> results = new ArrayList<>();

        int i = 0, j = 0;

        while (i < debtors.size() && j < creditors.size()) {

            var debtor = debtors.get(i);
            var creditor = creditors.get(j);

            double debt = -debtor.getValue();
            double credit = creditor.getValue();

            double transfer = Math.min(debt, credit);

            results.add(debtor.getKey() + " pays " + creditor.getKey() + " = " + transfer);

            debtor.setValue(debtor.getValue() + transfer);
            creditor.setValue(creditor.getValue() - transfer);

            if (Math.abs(debtor.getValue()) < 0.01) i++;
            if (Math.abs(creditor.getValue()) < 0.01) j++;
        }

        return results;
    }
}