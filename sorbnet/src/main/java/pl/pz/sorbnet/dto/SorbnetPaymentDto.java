package pl.pz.sorbnet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;

@XmlRootElement(name = "SorbnetPaymentRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "paymentId",
        "amount",
        "currency",
        "senderBankId",
        "receiverBankId",
        "senderAccount",
        "receiverAccount",
        "title",
        "status"
})
@Schema(
        name = "SorbnetPaymentRequest",
        description = "Żądanie XML utworzenia przelewu w systemie SORBNet."
)
public class SorbnetPaymentDto {

    @Schema(description = "Unikalny identyfikator przelewu.", example = "713e52f6-9fa2-4baf-a0a6-68b4dff987e7")
    private String paymentId;

    @Schema(description = "Kwota przelewu.", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Kod waluty przelewu.", example = "PLN")
    private String currency;

    @Schema(description = "Identyfikator banku nadawcy.", example = "BANK_A", requiredMode = Schema.RequiredMode.REQUIRED)
    private String senderBankId;

    @Schema(description = "Identyfikator banku odbiorcy.", example = "BANK_B", requiredMode = Schema.RequiredMode.REQUIRED)
    private String receiverBankId;

    @Schema(description = "Numer rachunku nadawcy.", example = "11111100000000000000000001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String senderAccount;

    @Schema(description = "Numer rachunku odbiorcy.", example = "22222200000000000000000002", requiredMode = Schema.RequiredMode.REQUIRED)
    private String receiverAccount;

    @Schema(description = "Tytuł przelewu.", example = "Rozrachunek rynku międzybankowego")
    private String title;

    @Schema(description = "Status wejściowy zlecenia.", example = "NEW")
    private String status;

    public SorbnetPaymentDto() {}

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSenderBankId() { return senderBankId; }
    public void setSenderBankId(String senderBankId) { this.senderBankId = senderBankId; }

    public String getReceiverBankId() { return receiverBankId; }
    public void setReceiverBankId(String receiverBankId) { this.receiverBankId = receiverBankId; }

    public String getSenderAccount() { return senderAccount; }
    public void setSenderAccount(String senderAccount) { this.senderAccount = senderAccount; }

    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}