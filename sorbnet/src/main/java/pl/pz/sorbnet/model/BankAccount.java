package pl.pz.sorbnet.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
@Schema(name = "BankAccount", description = "Rachunek banku w danym serwisie rozliczeniowym.")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "service_code", nullable = false, length = 32)
    @Schema(description = "Kod serwisu rozliczeniowego.", example = "SORBNET",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceCode;

    @Column(name = "bank_id", nullable = false, length = 50)
    @Schema(description = "Identyfikator banku.", example = "BANK_A",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String bankId;

    @Column(name = "bank_name", nullable = false)
    @Schema(description = "Nazwa banku.", example = "Bank A - Sorbnet",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 64, unique = true)
    @Schema(description = "Numer rachunku rozliczeniowego.", example = "SORBNET-A-00000000000000000001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountNumber;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    @Schema(description = "Bieżące saldo rachunku.", example = "10000000.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal balance;

    @Column(name = "debt_limit", nullable = false, precision = 19, scale = 2)
    @Schema(description = "Limit zadłużenia banku.", example = "0.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal debtLimit;

    @Column(name = "blocked", nullable = false)
    @Schema(description = "Czy bank jest zablokowany.", example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean blocked;

    @Column(name = "overlimit_since")
    @Schema(description = "Czas przekroczenia limitu zadłużenia.", nullable = true)
    private LocalDateTime overlimitSince;

    @Column(name = "blocked_at")
    @Schema(description = "Czas blokady banku.", nullable = true)
    private LocalDateTime blockedAt;

    public BankAccount() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }

    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

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