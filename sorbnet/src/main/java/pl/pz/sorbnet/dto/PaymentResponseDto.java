package pl.pz.sorbnet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;

@XmlRootElement(name = "SorbnetPaymentResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "paymentId",
        "status",
        "message",
        "senderBankId",
        "receiverBankId",
        "senderAccount",
        "receiverAccount",
        "amount",
        "settledAt"
})
@Schema(
        name = "SorbnetPaymentResponse",
        description = "Odpowiedź XML zwracana po przetworzeniu przelewu w systemie SORBNet."
)
public class PaymentResponseDto {

    
    @Schema(description = "Unikalny identyfikator przelewu.", example = "713e52f6-9fa2-4baf-a0a6-68b4dff987e7")
    private String paymentId;

    @Schema(description = "Status przetworzenia przelewu.", example = "SETTLED", allowableValues = {"SETTLED", "REJECTED", "GRIDLOCK_HELD"})
    private String status;

    @Schema(description = "Opis wyniku przetwarzania przelewu.", example = "Payment processed")
    private String message;

    @Schema(description = "Identyfikator banku nadawcy.", example = "BANK_A")
    private String senderBankId;

    @Schema(description = "Identyfikator banku odbiorcy.", example = "BANK_B")
    private String receiverBankId;

    @Schema(description = "Numer rachunku nadawcy.", example = "11111100000000000000000001")
    private String senderAccount;

    @Schema(description = "Numer rachunku odbiorcy.", example = "22222200000000000000000002")
    private String receiverAccount;

    @Schema(description = "Kwota przelewu.", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "Data i czas rozliczenia przelewu w formacie ISO-8601.", example = "2026-06-06T13:55:17.239227200")
    private String settledAt;

    public PaymentResponseDto() {}

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSenderBankId() { return senderBankId; }
    public void setSenderBankId(String senderBankId) { this.senderBankId = senderBankId; }

    public String getReceiverBankId() { return receiverBankId; }
    public void setReceiverBankId(String receiverBankId) { this.receiverBankId = receiverBankId; }

    public String getSenderAccount() { return senderAccount; }
    public void setSenderAccount(String senderAccount) { this.senderAccount = senderAccount; }

    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getSettledAt() { return settledAt; }
    public void setSettledAt(String settledAt) { this.settledAt = settledAt; }
}