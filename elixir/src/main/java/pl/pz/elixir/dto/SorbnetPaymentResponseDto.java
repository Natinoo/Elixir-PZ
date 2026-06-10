package pl.pz.elixir.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;

@XmlRootElement(name = "SorbnetPaymentResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "paymentId",
        "status",
        "message",
        "senderBankId",
        "receiverBankId",
        "senderAccount",
        "receiverAccount",
        "amount",
        "settledAt",
        "type"
})
public class SorbnetPaymentResponseDto {

    private String paymentId;
    private String status;
    private String message;
    private String senderBankId;
    private String receiverBankId;
    private String senderAccount;
    private String receiverAccount;
    private BigDecimal amount;
    private String settledAt;
    private String type;  // np. "SORBNET", "EXPRESS", "STANDARD"

    public SorbnetPaymentResponseDto() {
    }

    // Główny konstruktor
    public SorbnetPaymentResponseDto(String paymentId, String status, String message,
                                     String senderBankId, String receiverBankId,
                                     String senderAccount, String receiverAccount,
                                     BigDecimal amount, String settledAt, String type) {
        this.paymentId = paymentId;
        this.status = status;
        this.message = message;
        this.senderBankId = senderBankId;
        this.receiverBankId = receiverBankId;
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.amount = amount;
        this.settledAt = settledAt;
        this.type = type;
    }

    // Gettery i settery
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderBankId() {
        return senderBankId;
    }

    public void setSenderBankId(String senderBankId) {
        this.senderBankId = senderBankId;
    }

    public String getReceiverBankId() {
        return receiverBankId;
    }

    public void setReceiverBankId(String receiverBankId) {
        this.receiverBankId = receiverBankId;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public String getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(String receiverAccount) {
        this.receiverAccount = receiverAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(String settledAt) {
        this.settledAt = settledAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}