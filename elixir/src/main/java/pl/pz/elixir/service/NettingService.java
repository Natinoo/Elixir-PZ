package pl.pz.elixir.service;

import org.springframework.stereotype.Service;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.dto.NettingTransferDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NettingService {

    private static final BigDecimal ZERO_THRESHOLD = new BigDecimal("0.01");

    public List<String> calculateNetting(List<ElixirPaymentDto> payments) {
        return calculateNettingTransfers(payments, null, "ELIXIR").stream()
                .map(t -> t.getDebtorBankId() + " pays " + t.getCreditorBankId() + " = " + t.getAmount())
                .toList();
    }

    public List<NettingTransferDto> calculateNettingTransfers(List<ElixirPaymentDto> payments,
                                                              String sessionId,
                                                              String serviceCode) {
        Map<String, BigDecimal> balanceMap = new HashMap<>();
        String currency = "PLN";

        for (ElixirPaymentDto payment : payments) {
            String sender = payment.getSenderBankId();
            String receiver = payment.getReceiverBankId();
            BigDecimal amount = payment.getAmount();
            currency = payment.getCurrency() == null ? currency : payment.getCurrency();

            balanceMap.put(sender, balanceMap.getOrDefault(sender, BigDecimal.ZERO).subtract(amount));
            balanceMap.put(receiver, balanceMap.getOrDefault(receiver, BigDecimal.ZERO).add(amount));
        }

        List<Map.Entry<String, BigDecimal>> debtors = new ArrayList<>();
        List<Map.Entry<String, BigDecimal>> creditors = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : balanceMap.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(entry);
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(entry);
            }
        }

        debtors.sort(Comparator.comparing(Map.Entry::getValue));
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<NettingTransferDto> results = new ArrayList<>();
        int debtorIndex = 0;
        int creditorIndex = 0;
        int sequence = 1;

        while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
            Map.Entry<String, BigDecimal> debtor = debtors.get(debtorIndex);
            Map.Entry<String, BigDecimal> creditor = creditors.get(creditorIndex);

            BigDecimal debt = debtor.getValue().abs();
            BigDecimal credit = creditor.getValue();
            BigDecimal transferAmount = debt.min(credit).setScale(2, RoundingMode.HALF_UP);

            String transferId = "NET-" + (sessionId == null ? UUID.randomUUID() : sessionId) + "-" + sequence++;
            results.add(new NettingTransferDto(
                    transferId,
                    sessionId,
                    debtor.getKey(),
                    creditor.getKey(),
                    null,
                    null,
                    transferAmount,
                    currency,
                    serviceCode
            ));

            debtor.setValue(debtor.getValue().add(transferAmount));
            creditor.setValue(creditor.getValue().subtract(transferAmount));

            if (debtor.getValue().abs().compareTo(ZERO_THRESHOLD) < 0) {
                debtorIndex++;
            }
            if (creditor.getValue().abs().compareTo(ZERO_THRESHOLD) < 0) {
                creditorIndex++;
            }
        }

        return results;
    }
}