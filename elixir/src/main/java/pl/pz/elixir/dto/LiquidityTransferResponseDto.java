package pl.pz.elixir.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

import java.math.BigDecimal;

@XmlRootElement(name = "Document")
@XmlAccessorType(XmlAccessType.FIELD)
public class LiquidityTransferResponseDto {

    @XmlElement(name = "LiquidityCreditTransferResponse")
    private LiquidityCreditTransferResponse response = new LiquidityCreditTransferResponse();

    public LiquidityTransferResponseDto() {
    }

    public String getRequestId() {
        return response.trfSts.requestId;
    }

    public String getBankId() {
        return response.trfSts.bankId;
    }

    public String getSourceServiceCode() {
        return response.trfSts.sourceServiceCode;
    }

    public String getTargetServiceCode() {
        return response.trfSts.targetServiceCode;
    }

    public String getSourceAccount() {
        return response.trfSts.sourceAccount;
    }

    public String getTargetAccount() {
        return response.trfSts.targetAccount;
    }

    public BigDecimal getAmount() {
        return response.trfSts.amount == null ? null : response.trfSts.amount.value;
    }

    public String getCurrency() {
        return response.trfSts.amount == null ? null : response.trfSts.amount.currency;
    }

    public String getStatus() {
        return response.trfSts.status;
    }

    public String getMessage() {
        return response.trfSts.message;
    }

    public String getSettledAt() {
        return response.trfSts.settledAt;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LiquidityCreditTransferResponse {
        @XmlElement(name = "GrpHdr")
        private GroupHeader grpHdr = new GroupHeader();

        @XmlElement(name = "TrfSts")
        private TransferStatus trfSts = new TransferStatus();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GroupHeader {
        @XmlElement(name = "MsgId")
        private String msgId;

        @XmlElement(name = "CreDtTm")
        private String creDtTm;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TransferStatus {
        @XmlElement(name = "ReqId")
        private String requestId;

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

        @XmlElement(name = "Sts")
        private String status;

        @XmlElement(name = "Msg")
        private String message;

        @XmlElement(name = "SettledAt")
        private String settledAt;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ActiveCurrencyAndAmount {
        @XmlAttribute(name = "Ccy")
        private String currency;

        @XmlValue
        private BigDecimal value;
    }
}