package pl.pz.sorbnet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.math.BigDecimal;

@XmlRootElement(name = "Payment")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(name = "Payment", description = "Komunikat XML przekazujący przelew/netting do SORBNet.")
public class SorbnetPaymentDto {

    @XmlElement
    @Schema(description = "Unikalny identyfikator przelewu.", example = "SORB-20260527-0001")
    private String paymentId;

    @XmlElement
    @Schema(description = "Kwota przelewu.", example = "1250000.00")
    private BigDecimal amount;

    @XmlElement
    @Schema(description = "Kod waluty ISO 4217.", example = "PLN")
    private String currency;

    @XmlElement
    @Schema(description = "Identyfikator lub rachunek nadawcy.", example = "BANK_A")
    private String senderAccount;

    @XmlElement
    @Schema(description = "Identyfikator lub rachunek odbiorcy.", example = "BANK_B")
    private String receiverAccount;

    @XmlElement
    @Schema(description = "Tytuł przelewu.", example = "Netting SESSION_1")
    private String title;

    @XmlElement
    @Schema(description = "Status komunikatu wejściowego.", example = "NEW")
    private String status;

    public String getSenderBankId() { return senderAccount; }
    public String getReceiverBankId() { return receiverAccount; }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}