package pl.pz.elixirexpress.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @Column(name = "payment_id", nullable = false, updatable = false)
    private String paymentId;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "receiver_name")
    private String receiverName;

    /** IBAN klienta nadawcy. */
    @Column(name = "sender_account", nullable = false)
    private String senderAccount;

    /** IBAN klienta odbiorcy. */
    @Column(name = "receiver_account", nullable = false)
    private String receiverAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private String title;

    @Column(name = "sender_bank_id", nullable = false, length = 50)
    private String senderBankId;

    @Column(name = "receiver_bank_id", nullable = false, length = 50)
    private String receiverBankId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "held_reason")
    private String heldReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public Payment() {
    }

    public Payment(String paymentId,
                   String senderName,
                   String receiverName,
                   String senderAccount,
                   String receiverAccount,
                   BigDecimal amount,
                   String currency,
                   String title,
                   String senderBankId,
                   String receiverBankId,
                   PaymentStatus status,
                   String type) {
        this.paymentId = paymentId;
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.amount = amount;
        this.currency = currency;
        this.title = title;
        this.senderBankId = senderBankId;
        this.receiverBankId = receiverBankId;
        this.status = status;
        this.type = type;
    }

    @PrePersist
    public void prePersist() {
        if (paymentId == null || paymentId.isBlank()) {
            paymentId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PaymentStatus.QUEUED;
        }
        if (type == null || type.isBlank()) {
            type = "EXPRESS";
        }
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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

    public String getHeldReason() {
        return heldReason;
    }

    public void setHeldReason(String heldReason) {
        this.heldReason = heldReason;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}