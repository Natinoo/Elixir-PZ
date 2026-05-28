package pl.pz.elixir.dto;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.math.BigDecimal;

@XmlRootElement(name = "SorbnetPaymentResponse")
public class SorbnetPaymentResponseDto {

    private String paymentId;
    private String status;
    private String message;
    private String senderBankId;
    private String receiverBankId;
    private BigDecimal amount;
    private String settledAt;

    public SorbnetPaymentResponseDto() {
    }

    @XmlElement(name = "paymentId")
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    @XmlElement(name = "status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @XmlElement(name = "message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @XmlElement(name = "senderBankId")
    public String getSenderBankId() {
        return senderBankId;
    }

    public void setSenderBankId(String senderBankId) {
        this.senderBankId = senderBankId;
    }

    @XmlElement(name = "receiverBankId")
    public String getReceiverBankId() {
        return receiverBankId;
    }

    public void setReceiverBankId(String receiverBankId) {
        this.receiverBankId = receiverBankId;
    }

    @XmlElement(name = "amount")
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @XmlElement(name = "settledAt")
    public String getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(String settledAt) {
        this.settledAt = settledAt;
    }
}