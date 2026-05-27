package pl.pz.sorbnet.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
@Schema(
    name = "BankAccount",
    description = "Rachunek rozliczeniowy banku uczestniczącego w systemie SORBNet."
)
public class BankAccount {

    @Id
    @Schema(
        description = "Unikalny identyfikator banku w systemie.",
        example = "PKO"
    )
    private String bankId;

    @Schema(
        description = "Pełna nazwa banku uczestniczącego w systemie.",
        example = "PKO Bank Polski"
    )
    private String bankName;

    @Schema(
        description = "Bieżące saldo rachunku rozliczeniowego banku.",
        example = "15000000.00"
    )
    private BigDecimal balance;

    @Schema(
        description = "Indywidualny limit zadłużenia banku dopuszczalny w systemie.",
        example = "30000000.00"
    )
    private BigDecimal debtLimit;

    @Schema(
        description = "Flaga informująca, czy bank jest aktualnie zablokowany i nie może uczestniczyć w rozrachunku.",
        example = "false"
    )
    private boolean blocked;

    @Schema(
        description = "Znacznik czasu momentu przekroczenia limitu zadłużenia. Pole puste, jeśli bank mieści się w limicie.",
        example = "2026-05-27T15:00:00",
        nullable = true
    )
    private LocalDateTime overlimitSince;

    @Schema(
        description = "Znacznik czasu blokady banku. Pole puste, jeśli bank nie jest zablokowany.",
        example = "2026-05-27T17:00:00",
        nullable = true
    )
    private LocalDateTime blockedAt;

    public BankAccount() {}

    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getDebtLimit() { return debtLimit; }
    public void setDebtLimit(BigDecimal debtLimit) { this.debtLimit = debtLimit; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public LocalDateTime getOverlimitSince() { return overlimitSince; }
    public void setOverlimitSince(LocalDateTime overlimitSince) { this.overlimitSince = overlimitSince; }

    public LocalDateTime getBlockedAt() { return blockedAt; }
    public void setBlockedAt(LocalDateTime blockedAt) { this.blockedAt = blockedAt; }
}