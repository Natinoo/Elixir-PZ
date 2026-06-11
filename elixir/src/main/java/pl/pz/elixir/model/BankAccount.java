package pl.pz.elixir.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bank_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bank_account_service_bank", columnNames = {"service_code", "bank_id"}),
                @UniqueConstraint(name = "uk_bank_account_number", columnNames = {"account_number"})
        }
)
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code", nullable = false, length = 32)
    private String serviceCode;

    @Column(name = "bank_id", nullable = false, length = 50)
    private String bankId;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 64)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "debt_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal debtLimit;

    @Column(nullable = false)
    private boolean blocked;

    @Column(name = "overlimit_since")
    private LocalDateTime overlimitSince;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    public BankAccount() {
    }

    public BankAccount(String serviceCode, String bankId, String bankName, String accountNumber,
                       BigDecimal balance, BigDecimal debtLimit, boolean blocked) {
        this.serviceCode = serviceCode;
        this.bankId = bankId;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.debtLimit = debtLimit;
        this.blocked = blocked;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
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
}
