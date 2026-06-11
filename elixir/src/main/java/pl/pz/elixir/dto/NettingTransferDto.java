package pl.pz.elixir.dto;

import java.math.BigDecimal;

public class NettingTransferDto {

    private String transferId;
    private String sessionId;
    private String debtorBankId;
    private String creditorBankId;
    private String debtorAccount;
    private String creditorAccount;
    private BigDecimal amount;
    private String currency;
    private String serviceCode;

    public NettingTransferDto() {
    }

    public NettingTransferDto(String transferId, String sessionId, String debtorBankId, String creditorBankId,
                              String debtorAccount, String creditorAccount, BigDecimal amount, String currency,
                              String serviceCode) {
        this.transferId = transferId;
        this.sessionId = sessionId;
        this.debtorBankId = debtorBankId;
        this.creditorBankId = creditorBankId;
        this.debtorAccount = debtorAccount;
        this.creditorAccount = creditorAccount;
        this.amount = amount;
        this.currency = currency;
        this.serviceCode = serviceCode;
    }

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDebtorBankId() {
        return debtorBankId;
    }

    public void setDebtorBankId(String debtorBankId) {
        this.debtorBankId = debtorBankId;
    }

    public String getCreditorBankId() {
        return creditorBankId;
    }

    public void setCreditorBankId(String creditorBankId) {
        this.creditorBankId = creditorBankId;
    }

    public String getDebtorAccount() {
        return debtorAccount;
    }

    public void setDebtorAccount(String debtorAccount) {
        this.debtorAccount = debtorAccount;
    }

    public String getCreditorAccount() {
        return creditorAccount;
    }

    public void setCreditorAccount(String creditorAccount) {
        this.creditorAccount = creditorAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }
}