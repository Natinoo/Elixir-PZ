package pl.pz.elixir.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "settlement_bank_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_settlement_account_service_bank_default", columnNames = {"service_code", "bank_id", "is_default"}),
                @UniqueConstraint(name = "uk_settlement_account_number", columnNames = {"account_number"})
        }
)
public class SettlementBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code", nullable = false, length = 32)
    private String serviceCode;

    @Column(name = "bank_id", nullable = false)
    private String bankId;

    @Column(name = "account_number", nullable = false, length = 64)
    private String accountNumber;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    public SettlementBankAccount() {
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}

