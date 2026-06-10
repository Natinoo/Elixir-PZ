package pl.pz.elixir.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    private String bankId;

    private String bankName;
    private Double balance;
    private Double debtLimit;
    private boolean blocked;
    private LocalDateTime overlimitSince;
    private LocalDateTime blockedAt;

    public BankAccount() {}

    public BankAccount(String bankId, String bankName, Double balance, Double debtLimit, boolean blocked) {
        this.bankId = bankId;
        this.bankName = bankName;
        this.balance = balance;
        this.debtLimit = debtLimit;
        this.blocked = blocked;
    }

    // Gettery i settery
    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public Double getDebtLimit() { return debtLimit; }
    public void setDebtLimit(Double debtLimit) { this.debtLimit = debtLimit; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public LocalDateTime getOverlimitSince() { return overlimitSince; }
    public void setOverlimitSince(LocalDateTime overlimitSince) { this.overlimitSince = overlimitSince; }

    public LocalDateTime getBlockedAt() { return blockedAt; }
    public void setBlockedAt(LocalDateTime blockedAt) { this.blockedAt = blockedAt; }
}