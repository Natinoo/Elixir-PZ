package pl.pz.sorbnet.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Schema(
        name = "Payment",
        description = "Przelew przetwarzany w systemie SORBNet, w tym przelew płynnościowy i rozrachunkowy."
)
public class Payment {

    @Id
    @Column(name = "payment_id", nullable = false, updatable = false)
    @Schema(
            description = "Unikalny identyfikator przelewu w systemie SORBNet.",
            example = "SORB-20260527-000001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String paymentId;

    @Column(name = "sender_bank_id", nullable = false)
    @Schema(
            description = "Identyfikator banku nadawcy przelewu.",
            example = "PKO",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String senderBankId;

    @Column(name = "receiver_bank_id", nullable = false)
    @Schema(
            description = "Identyfikator banku odbiorcy przelewu.",
            example = "PEKAO",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String receiverBankId;

    @Column(name = "sender_account", nullable = false)
    @Schema(
            description = "Numer rachunku nadawcy, z którego inicjowany jest przelew.",
            example = "12102010260000042270201111",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String senderAccount;

    @Column(name = "receiver_account", nullable = false)
    @Schema(
            description = "Numer rachunku odbiorcy przelewu.",
            example = "47114020040000300201355387",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String receiverAccount;

    @Column(name = "sender_name")
    @Schema(
            description = "Nazwa lub imię i nazwisko nadawcy.",
            example = "Jan Kowalski",
            nullable = true
    )
    private String senderName;

    @Column(name = "receiver_name")
    @Schema(
            description = "Nazwa lub imię i nazwisko odbiorcy.",
            example = "Anna Nowak",
            nullable = true
    )
    private String receiverName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @Schema(
            description = "Kwota przelewu wyrażona w walucie rozrachunku.",
            example = "12500000.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Schema(
            description = "Kod waluty przelewu.",
            example = "PLN",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String currency;

    @Column(name = "title", nullable = false)
    @Schema(
            description = "Tytuł przelewu przekazywany w komunikacie płatniczym.",
            example = "Rozrachunek międzybankowy",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String title;

    @Column(name = "source_service", length = 32)
    @Schema(
            description = "Serwis źródłowy powiązany z przelewem, np. SORBNET, ELIXIR, ELIXIR_EXPRESS.",
            example = "ELIXIR",
            nullable = true
    )
    private String sourceService;

    @Column(name = "payment_type", length = 32)
    @Schema(
            description = "Typ przelewu z perspektywy biznesowej.",
            example = "LIQUIDITY_TRANSFER",
            nullable = true
    )
    private String paymentType;

    @Column(name = "session_id")
    @Schema(
            description = "Identyfikator sesji ELIXIR / ELIXIR EXPRESS powiązanej z przelewem.",
            example = "ELIXIR-SESSION-12345",
            nullable = true
    )
    private String sessionId;

    @Column(name = "liquidity_request_id")
    @Schema(
            description = "Identyfikator requestu płynnościowego, jeśli przelew został wykonany w odpowiedzi na liquidity request.",
            example = "LIQ-REQ-20260611-0001",
            nullable = true
    )
    private String liquidityRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Schema(
            description = "Bieżący status przetwarzania przelewu.",
            implementation = PaymentStatus.class,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    @Schema(
            description = "Znacznik czasu utworzenia przelewu w systemie.",
            example = "2026-05-27T10:15:30",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime createdAt;

    @Column(name = "settled_at")
    @Schema(
            description = "Znacznik czasu rozliczenia przelewu.",
            example = "2026-05-27T10:15:32",
            nullable = true
    )
    private LocalDateTime settledAt;

    @Column(name = "rejection_reason", length = 512)
    @Schema(
            description = "Powód odrzucenia przelewu. Pole uzupełniane wyłącznie dla statusu REJECTED.",
            example = "Insufficient liquidity on settlement account",
            nullable = true
    )
    private String rejectionReason;

    public Payment() {
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getSenderBankId() { return senderBankId; }
    public void setSenderBankId(String senderBankId) { this.senderBankId = senderBankId; }

    public String getReceiverBankId() { return receiverBankId; }
    public void setReceiverBankId(String receiverBankId) { this.receiverBankId = receiverBankId; }

    public String getSenderAccount() { return senderAccount; }
    public void setSenderAccount(String senderAccount) { this.senderAccount = senderAccount; }

    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getLiquidityRequestId() { return liquidityRequestId; }
    public void setLiquidityRequestId(String liquidityRequestId) { this.liquidityRequestId = liquidityRequestId; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}