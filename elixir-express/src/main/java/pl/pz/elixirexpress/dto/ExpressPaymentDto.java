package pl.pz.elixirexpress.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;

@XmlRootElement(name = "Payment")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "paymentId",
        "amount",
        "currency",
        "senderName",
        "receiverName",
        "senderBankId",
        "receiverBankId",
        "senderAccount",
        "receiverAccount",
        "title",
        "type"
})
public class ExpressPaymentDto {

    private String paymentId;
    private BigDecimal amount;
    private String currency;

    private String senderName;
    private String receiverName;

    /** IBAN klienta nadawcy. */
    private String senderAccount;

    /** IBAN klienta odbiorcy. */
    private String receiverAccount;

    private String title;
    private String senderBankId;
    private String receiverBankId;
    private String type;

    public ExpressPaymentDto() {
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}