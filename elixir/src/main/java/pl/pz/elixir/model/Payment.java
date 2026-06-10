package pl.pz.elixir.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment implements Persistable<String> {

    @Id
    @Column(name = "payment_id", nullable = false, updatable = false)
    private String paymentId;

    @Column(name = "sender_bank_id", nullable = false)
    private String senderBankId;

    @Column(name = "receiver_bank_id", nullable = false)
    private String receiverBankId;

    @Column(name = "sender_account", nullable = false)
    private String senderAccount;

    @Column(name = "receiver_account", nullable = false)
    private String receiverAccount;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "type")
    private String type;  // np. "STANDARD", "EXPRESS", "SORBNET"

    @Transient
    private boolean isNew = true;

    public Payment() {
    }

    public Payment(
        String paymentId,
        String senderBankId,
        String receiverBankId,
        String senderAccount,
        String receiverAccount,
        Double amount,
        String currency,
        String title,
        PaymentStatus status,
        LocalDateTime createdAt,
        String type
    ) {
        this.paymentId = paymentId;
        this.senderBankId = senderBankId;
        this.receiverBankId = receiverBankId;
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.amount = amount;
        this.currency = currency;
        this.title = title;
        this.status = status;
        this.createdAt = createdAt;
        this.type = type;
        this.isNew = true;
    }

    @PrePersist
    public void prePersist() {
        if (this.paymentId == null || this.paymentId.isBlank()) {
            this.paymentId = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = PaymentStatus.QUEUED;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @PostPersist
    @PostLoad
    public void markNotNew() {
        this.isNew = false;
    }

    @Override
    public String getId() {
        return paymentId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    // Gettery i settery
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}