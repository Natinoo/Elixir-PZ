package pl.pz.sorbnet.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request płynnościowy otrzymany z ELIXIR / ELIXIR EXPRESS.
 * Czeka w stanie PENDING na decyzję operatora banku w GUI SORBNET
 * (operator "wyklikuje" przelew zasilający techniczne konto banku w ELIXIR-ze).
 */
@Entity
@Table(name = "liquidity_requests")
@Schema(
        name = "LiquidityRequest",
        description = "Request płynnościowy otrzymany przez SORBNet z systemu ELIXIR lub ELIXIR EXPRESS."
)
public class LiquidityRequest {

    @Id
    @Column(name = "request_id", nullable = false, updatable = false, length = 64)
    @Schema(
            description = "Identyfikator requestu nadany przez serwis źródłowy (ReqId). Zapewnia idempotencję.",
            example = "LIQ-REQ-20260611-0001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String requestId;

    @Column(name = "original_message_id", length = 128)
    @Schema(
            description = "Identyfikator komunikatu z nagłówka ISO 20022 (GrpHdr.MsgId).",
            example = "MSG-20260611-0001",
            nullable = true
    )
    private String originalMessageId;

    @Column(name = "bank_id", nullable = false, length = 50)
    @Schema(
            description = "Identyfikator banku, którego request dotyczy.",
            example = "BANK_A",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String bankId;

    @Column(name = "requesting_service_code", nullable = false, length = 32)
    @Schema(
            description = "Serwis proszący o płynność: ELIXIR albo ELIXIR_EXPRESS.",
            example = "ELIXIR",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String requestingServiceCode;

    @Column(name = "session_id", length = 64)
    @Schema(
            description = "Identyfikator sesji ELIXIR oczekującej na płynność.",
            example = "ELIXIR-SESSION-12345",
            nullable = true
    )
    private String sessionId;

    @Column(name = "source_account", length = 64)
    @Schema(
            description = "Rachunek banku w SORBNET do obciążenia.",
            example = "12102010260000042270201111",
            nullable = true
    )
    private String sourceAccount;

    @Column(name = "target_account", nullable = false, length = 64)
    @Schema(
            description = "Techniczne konto banku w serwisie ELIXIR do zasilenia.",
            example = "47114020040000300201355387",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String targetAccount;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @Schema(
            description = "Kwota żądanej płynności.",
            example = "2500000.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Schema(
            description = "Waluta requestu płynnościowego.",
            example = "PLN",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String currency;

    @Column(name = "message", length = 512)
    @Schema(
            description = "Komunikat lub powód requestu, np. brak płynności w sesji.",
            example = "Brak płynności w sesji ELIXIR-SESSION-12345",
            nullable = true
    )
    private String message;

    @Column(name = "source_has_funds")
    @Schema(
            description = "Informacja z ELIXIR-a, czy według jego lokalnego lustra konto SORBNET banku ma środki.",
            example = "false",
            nullable = true
    )
    private Boolean sourceHasFunds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Schema(
            description = "Bieżący status requestu płynnościowego.",
            implementation = LiquidityRequestStatus.class,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LiquidityRequestStatus status;

    @Column(name = "received_at", nullable = false)
    @Schema(
            description = "Znacznik czasu odebrania requestu przez SORBNet.",
            example = "2026-06-11T17:15:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    @Schema(
            description = "Znacznik czasu przetworzenia requestu przez operatora.",
            example = "2026-06-11T17:20:00",
            nullable = true
    )
    private LocalDateTime processedAt;

    @Column(name = "payment_id", length = 64)
    @Schema(
            description = "Identyfikator przelewu SORBNET wykonanego dla tego requestu.",
            example = "LIQ-8f6c7d12-4f2a-4b0f-9f0f-12b0c9d9e001",
            nullable = true
    )
    private String paymentId;

    public LiquidityRequest() {
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getOriginalMessageId() {
        return originalMessageId;
    }

    public void setOriginalMessageId(String originalMessageId) {
        this.originalMessageId = originalMessageId;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }

    public String getRequestingServiceCode() {
        return requestingServiceCode;
    }

    public void setRequestingServiceCode(String requestingServiceCode) {
        this.requestingServiceCode = requestingServiceCode;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public String getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(String targetAccount) {
        this.targetAccount = targetAccount;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getSourceHasFunds() {
        return sourceHasFunds;
    }

    public void setSourceHasFunds(Boolean sourceHasFunds) {
        this.sourceHasFunds = sourceHasFunds;
    }

    public LiquidityRequestStatus getStatus() {
        return status;
    }

    public void setStatus(LiquidityRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
}