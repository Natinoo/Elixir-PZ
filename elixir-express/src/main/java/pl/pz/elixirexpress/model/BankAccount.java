package pl.pz.elixirexpress.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @Column(name = "bank_id", nullable = false, length = 50)
    private String bankId;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "debt_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal debtLimit = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean blocked = false;

    @Column(name = "overlimit_since")
    private LocalDateTime overlimitSince;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    public BankAccount() {
    }

    public BankAccount(String bankId, String bankName, BigDecimal balance, BigDecimal debtLimit, boolean blocked) {
        this.bankId = bankId;
        this.bankName = bankName;
        this.balance = balance;
        this.debtLimit = debtLimit;
        this.blocked = blocked;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getDebtLimit() {
        return debtLimit;
    }

    public void setDebtLimit(BigDecimal debtLimit) {
        this.debtLimit = debtLimit;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public LocalDateTime getOverlimitSince() {
        return overlimitSince;
    }

    public void setOverlimitSince(LocalDateTime overlimitSince) {
        this.overlimitSince = overlimitSince;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }

    public BigDecimal lowestAllowedBalance() {
        return debtLimit == null ? BigDecimal.ZERO : debtLimit.negate();
    }

    public boolean isOverLimit() {
        return balance != null && balance.compareTo(lowestAllowedBalance()) < 0;
    }
}