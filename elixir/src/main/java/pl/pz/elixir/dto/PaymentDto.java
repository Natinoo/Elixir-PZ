package pl.pz.elixir.dto;

public class PaymentDto {
    private String paymentId;
    private Double amount;
    private String currency;
    private String senderAccount;
    private String receiverAccount;
    private String title;
    private Boolean urgent;

    public PaymentDto() {
    }

    public PaymentDto(String paymentId, Double amount, String currency, String senderAccount,
                      String receiverAccount, String title, Boolean urgent) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.title = title;
        this.urgent = urgent;
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

    public Boolean getUrgent() {
        return urgent;
    }

    public void setUrgent(Boolean urgent) {
        this.urgent = urgent;
    }
}