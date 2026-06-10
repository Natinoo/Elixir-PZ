package pl.pz.elixirexpress.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Payment")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "paymentId",
        "amount",
        "currency",
        "senderAccount",
        "receiverAccount",
        "title",
        "senderBankId",
        "receiverBankId",
        "type"
})
public class ExpressPaymentDto {

    private String paymentId;
    private Double amount;
    private String currency;
    private String senderAccount;
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

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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