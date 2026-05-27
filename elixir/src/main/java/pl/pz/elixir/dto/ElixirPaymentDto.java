package pl.pz.elixir.dto;

import jakarta.xml.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


@XmlRootElement(name = "Payment")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "paymentId",
        "amount",
        "currency",
        "senderAccount",
        "receiverAccount",
        "title"
})
public class ElixirPaymentDto {

    private String paymentId;

    @NotNull(message = "Kwota jest wymagana")
    @Positive(message = "Kwota musi być większa od zera")
    private Double amount;

    @NotBlank(message = "Waluta jest wymagana")
    private String currency;

    @NotBlank(message = "Bank nadawcy jest wymagany")
    private String senderAccount;

    @NotBlank(message = "Bank odbiorcy jest wymagany")
    private String receiverAccount;

    @NotBlank(message = "Tytuł jest wymagany")
    private String title;

    public ElixirPaymentDto() {
    }

    public ElixirPaymentDto(String paymentId, Double amount, String currency, String senderAccount,
                            String receiverAccount, String title) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.title = title;
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
}