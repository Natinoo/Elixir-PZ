package pl.pz.sorbnet.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Schema(
    name = "Payment",
    description = "Przelew międzybankowy przetwarzany w systemie RTGS SORBNet."
)
public class Payment {

    @Id
    @Schema(
        description = "Unikalny identyfikator przelewu w systemie SORBNet.",
        example = "SORB-20260527-000001"
    )
    private String paymentId;

    @Schema(
        description = "Identyfikator banku nadawcy przelewu.",
        example = "PKO"
    )
    private String senderBankId;

    @Schema(
        description = "Identyfikator banku odbiorcy przelewu.",
        example = "PEKAO"
    )
    private String receiverBankId;

    @Schema(
        description = "Kwota przelewu wyrażona w walucie rozrachunku.",
        example = "12500000.00"
    )
    private BigDecimal amount;

    @Schema(
        description = "Kod waluty przelewu.",
        example = "PLN"
    )
    private String currency;

    @Schema(
        description = "Numer rachunku nadawcy, z którego inicjowany jest przelew.",
        example = "12102010260000042270201111"
    )
    private String senderAccount;

    @Schema(
        description = "Numer rachunku odbiorcy przelewu.",
        example = "47114020040000300201355387"
    )
    private String receiverAccount;

    @Schema(
        description = "Tytuł przelewu przekazywany w komunikacie płatniczym.",
        example = "Rozrachunek międzybankowy"
    )
    private String title;

    @Enumerated(EnumType.STRING)
    @Schema(
        description = """
            Bieżący status przetwarzania przelewu.
            Możliwe wartości:
            - SETTLED: przelew został rozliczony,
            - REJECTED: przelew został odrzucony,
            - GRIDLOCK_HELD: przelew został wstrzymany w kolejce gridlock.
            """,
        implementation = PaymentStatus.class
    )
    private PaymentStatus status;

    @Schema(
        description = "Znacznik czasu utworzenia przelewu w systemie.",
        example = "2026-05-27T10:15:30"
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "Znacznik czasu rozliczenia przelewu. Pole puste, jeśli przelew nie został jeszcze rozliczony.",
        example = "2026-05-27T10:15:32",
        nullable = true
    )
    private LocalDateTime settledAt;

    @Schema(
        description = "Powód odrzucenia przelewu. Pole uzupełniane wyłącznie dla statusu REJECTED.",
        example = "Insufficient liquidity on settlement account",
        nullable = true
    )
    private String rejectionReason;

    public Payment() {}

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getSenderBankId() { return senderBankId; }
    public void setSenderBankId(String senderBankId) { this.senderBankId = senderBankId; }

    public String getReceiverBankId() { return receiverBankId; }
    public void setReceiverBankId(String receiverBankId) { this.receiverBankId = receiverBankId; }

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

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}