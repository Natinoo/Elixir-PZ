package pl.pz.sorbnet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    private String bankId;
    private String bankName;
    private BigDecimal balance;
    private BigDecimal debtLimit;
    private boolean blocked;
    private LocalDateTime overlimitSince;
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