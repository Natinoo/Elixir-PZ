package pl.pz.sorbnet.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@JacksonXmlRootElement(localName = "SorbnetPaymentResponse")
@Schema(
    name = "SorbnetPaymentResponse",
    description = "Odpowiedź XML zwracana po przetworzeniu przelewu w systemie SORBNet."
)
public class PaymentResponseDto {

    @JacksonXmlProperty(localName = "paymentId")
    @Schema(description = "Identyfikator przelewu.", example = "SORB-20260527-0001")
    private String paymentId;

    @JacksonXmlProperty(localName = "status")
    @Schema(
        description = "Status przetwarzania przelewu.",
        example = "SETTLED",
        allowableValues = {"SETTLED", "REJECTED", "GRIDLOCK_HELD"}
    )
    private String status;

    @JacksonXmlProperty(localName = "message")
    @Schema(description = "Opis wyniku operacji lub dodatkowa informacja biznesowa.", example = "Przelew został rozliczony")
    private String message;

    @JacksonXmlProperty(localName = "senderBankId")
    @Schema(description = "Identyfikator banku nadawcy.", example = "PKO")
    private String senderBankId;

    @JacksonXmlProperty(localName = "receiverBankId")
    @Schema(description = "Identyfikator banku odbiorcy.", example = "PEKAO")
    private String receiverBankId;

    @JacksonXmlProperty(localName = "senderAccount")
    @Schema(description = "Numer rachunku nadawcy.", example = "11111100000000000000000001")
    private String senderAccount;

    @JacksonXmlProperty(localName = "receiverAccount")
    @Schema(description = "Numer rachunku odbiorcy.", example = "22222200000000000000000002")
    private String receiverAccount;

    @JacksonXmlProperty(localName = "amount")
    @Schema(description = "Kwota przelewu.", example = "1250000.00")
    private BigDecimal amount;

    @JacksonXmlProperty(localName = "settledAt")
    @Schema(description = "Znacznik czasu rozliczenia przelewu, jeśli został rozliczony.", example = "2026-05-27T17:45:21")
    private String settledAt;

    public PaymentResponseDto() {
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(String settledAt) {
        this.settledAt = settledAt;
    }
    public String getSenderAccount() { return senderAccount; }
    public void setSenderAccount(String senderAccount) { this.senderAccount = senderAccount; }

    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }
}