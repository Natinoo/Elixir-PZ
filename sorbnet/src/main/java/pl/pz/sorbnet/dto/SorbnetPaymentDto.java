package pl.pz.sorbnet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.math.BigDecimal;

@XmlRootElement(name = "SorbnetPaymentRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(name = "SorbnetPaymentRequest", description = "Komunikat XML przekazujący przelew do SORBNet.")
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
    @Schema(description = "Identyfikator banku nadawcy (używany do rozrachunku).", example = "PKO")
    private String senderBankId;

    @XmlElement
    @Schema(description = "Identyfikator banku odbiorcy (używany do rozrachunku).", example = "PEKAO")
    private String receiverBankId;

    @XmlElement
    @Schema(description = "Numer rachunku nadawcy.", example = "12102010260000042270201111")
    private String senderAccount;

    @XmlElement
    @Schema(description = "Numer rachunku odbiorcy.", example = "47114020040000300201355387")
    private String receiverAccount;

    @XmlElement
    @Schema(description = "Tytuł przelewu.", example = "Rozrachunek rynku międzybankowego")
    private String title;

    @XmlElement
    @Schema(description = "Status komunikatu wejściowego.", example = "NEW")
    private String status;

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