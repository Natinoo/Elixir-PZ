package pl.pz.sorbnet.dto;

import java.math.BigDecimal;
import jakarta.xml.bind.annotation.*;

@XmlRootElement(name = "ElixirPaymentDto")  
@XmlAccessorType(XmlAccessType.FIELD)
public class SorbnetPaymentDto {

    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private String senderAccount;   
    private String receiverAccount; 
    private String title;
    private String status;

    // senderBankId i receiverBankId wyprowadzamy z senderAccount/receiverAccount
    public String getSenderBankId() { return senderAccount; }
    public String getReceiverBankId() { return receiverAccount; }

    // --- gettery/settery ---
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getSenderAccount() { return senderAccount; }
    public void setSenderAccount(String senderAccount) { this.senderAccount = senderAccount; }
    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
