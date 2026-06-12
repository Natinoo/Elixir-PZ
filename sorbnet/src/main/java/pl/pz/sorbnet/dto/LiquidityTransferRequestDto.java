package pl.pz.sorbnet.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

import java.math.BigDecimal;

/**
 * Request płynnościowy wysyłany przez ELIXIR / ELIXIR EXPRESS do SORBNET-u
 * (SessionService.buildLiquidityRequest), gdy bank-dłużnik nie ma środków
 * na lokalne rozliczenie sesji nettingowej.
 *
 * Semantyka pól (zgodnie z konstruktorem po stronie ELIXIR-a):
 * - SourceServiceCode = "SORBNET" (skąd mają przyjść środki),
 * - TargetServiceCode = "ELIXIR" / "ELIXIR_EXPRESS" (serwis, który prosi),
 * - SourceAccount = rachunek banku w SORBNET (do obciążenia),
 * - TargetAccount = techniczne konto banku w serwisie ELIXIR (do zasilenia),
 * - SourceHasFunds = czy wg lokalnego lustra ELIXIR-a rachunek SORBNET ma środki.
 *
 */
@XmlRootElement(name = "Document")
@XmlAccessorType(XmlAccessType.FIELD)
public class LiquidityTransferRequestDto {

    @XmlElement(name = "LiquidityCreditTransferRequest")
    private LiquidityCreditTransferRequest request = new LiquidityCreditTransferRequest();

    public LiquidityTransferRequestDto() {
    }

    public String getRequestId() {
        return trf().requestId;
    }

    public String getSessionId() {
        return trf().sessionId;
    }

    public String getBankId() {
        return trf().bankId;
    }

    /** Źródło środków — z perspektywy ELIXIR-a zawsze SORBNET. */
    public String getSourceServiceCode() {
        return trf().sourceServiceCode;
    }

    /** Serwis, który prosi o płynność: ELIXIR albo ELIXIR_EXPRESS. */
    public String getTargetServiceCode() {
        return trf().targetServiceCode;
    }

    /** Rachunek banku w SORBNET, który ma zostać obciążony. */
    public String getSourceAccount() {
        return trf().sourceAccount;
    }

    /** Techniczne konto banku w serwisie ELIXIR, które ma zostać zasilone. */
    public String getTargetAccount() {
        return trf().targetAccount;
    }

    public BigDecimal getAmount() {
        return trf().amount == null ? null : trf().amount.value;
    }

    public String getCurrency() {
        return trf().amount == null ? null : trf().amount.currency;
    }

    public String getMessage() {
        return trf().message;
    }

    /** Informacja ELIXIR-a, czy wg jego lustra konto SORBNET banku ma środki. */
    public Boolean getSourceHasFunds() {
        return trf().sourceHasFunds;
    }

    public String getPaymentId() {
    return trf().paymentId;
    }

    private Transfer trf() {
        if (request == null) {
            request = new LiquidityCreditTransferRequest();
        }
        if (request.trf == null) {
            request.trf = new Transfer();
        }
        return request.trf;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LiquidityCreditTransferRequest {
        @XmlElement(name = "GrpHdr")
        private GroupHeader grpHdr = new GroupHeader();

        @XmlElement(name = "TrfInstr")
        private Transfer trf = new Transfer();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GroupHeader {
        @XmlElement(name = "MsgId")
        private String msgId;

        @XmlElement(name = "CreDtTm")
        private String creDtTm;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Transfer {
        @XmlElement(name = "ReqId")
        private String requestId;

        @XmlElement(name = "SessionId")
        private String sessionId;

        @XmlElement(name = "PaymentId")   
        private String paymentId;

        @XmlElement(name = "BankId")
        private String bankId;

        @XmlElement(name = "SourceServiceCode")
        private String sourceServiceCode;

        @XmlElement(name = "TargetServiceCode")
        private String targetServiceCode;

        @XmlElement(name = "SourceAccount")
        private String sourceAccount;

        @XmlElement(name = "TargetAccount")
        private String targetAccount;

        @XmlElement(name = "Amt")
        private ActiveCurrencyAndAmount amount = new ActiveCurrencyAndAmount();

        @XmlElement(name = "Reason")
        private String message;

        @XmlElement(name = "SourceHasFunds")
        private Boolean sourceHasFunds;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ActiveCurrencyAndAmount {
        @XmlAttribute(name = "Ccy")
        private String currency;

        @XmlValue
        private BigDecimal value;
    }
}