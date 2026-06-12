package pl.pz.sorbnet.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Odpowiedź SORBNET-u na request płynnościowy.
 * Root: <Document><LiquidityCreditTransferResponse>...
 *
 * Struktura 1:1 zgodna z pl.pz.elixir.dto.LiquidityTransferResponseDto,
 * którą ELIXIR / ELIXIR EXPRESS parsują po stronie konsumenta.
 */
@XmlRootElement(name = "Document")
@XmlAccessorType(XmlAccessType.FIELD)
public class LiquidityTransferResponseDto {

    @XmlElement(name = "LiquidityCreditTransferResponse")
    private LiquidityCreditTransferResponse response = new LiquidityCreditTransferResponse();

    public LiquidityTransferResponseDto() {
    }

    public LiquidityTransferResponseDto(String requestId) {
        setRequestId(requestId);
        response.grpHdr.msgId = "LIQRESP-" + requestId;
        response.grpHdr.creDtTm = LocalDateTime.now().toString();
    }

    public String getRequestId() {
        return trf().requestId;
    }

    public void setRequestId(String requestId) {
        trf().requestId = requestId;
    }

    public String getBankId() {
        return trf().bankId;
    }

    public void setBankId(String bankId) {
        trf().bankId = bankId;
    }

    public String getSourceServiceCode() {
        return trf().sourceServiceCode;
    }

    public void setSourceServiceCode(String sourceServiceCode) {
        trf().sourceServiceCode = sourceServiceCode;
    }

    public String getTargetServiceCode() {
        return trf().targetServiceCode;
    }

    public void setTargetServiceCode(String targetServiceCode) {
        trf().targetServiceCode = targetServiceCode;
    }

    public String getSourceAccount() {
        return trf().sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        trf().sourceAccount = sourceAccount;
    }

    public String getTargetAccount() {
        return trf().targetAccount;
    }

    public void setTargetAccount(String targetAccount) {
        trf().targetAccount = targetAccount;
    }

    public BigDecimal getAmount() {
        return trf().amount == null ? null : trf().amount.value;
    }

    public void setAmount(BigDecimal amount, String currency) {
        if (trf().amount == null) {
            trf().amount = new ActiveCurrencyAndAmount();
        }
        trf().amount.value = amount;
        trf().amount.currency = currency != null ? currency : "PLN";
    }

    public String getStatus() {
        return trf().status;
    }

    public void setStatus(String status) {
        trf().status = status;
    }

    public String getMessage() {
        return trf().message;
    }

    public void setMessage(String message) {
        trf().message = message;
    }

    public String getSettledAt() {
        return trf().settledAt;
    }

    public void setSettledAt(String settledAt) {
        trf().settledAt = settledAt;
    }
    public String getPaymentId() { return trf().paymentId; }

    public void setPaymentId(String paymentId) { trf().paymentId = paymentId; }

    private TransferStatus trf() {
        if (response == null) {
            response = new LiquidityCreditTransferResponse();
        }
        if (response.trfSts == null) {
            response.trfSts = new TransferStatus();
        }
        return response.trfSts;
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
        @XmlElement(name = "OrgnlTxId")
        private String requestId;

        @XmlElement(name = "BankId")
        private String bankId;

        @XmlElement(name = "SourceServiceCode")
        private String sourceServiceCode;

        @XmlElement(name = "TargetServiceCode")
        private String targetServiceCode;

        @XmlElement(name = "SourceAccount")
        private String sourceAccount;

        @XmlElement(name = "PaymentId")   
        private String paymentId;

        @XmlElement(name = "TargetAccount")
        private String targetAccount;

        @XmlElement(name = "Amt")
        private ActiveCurrencyAndAmount amount = new ActiveCurrencyAndAmount();

        @XmlElement(name = "TxSts")
        private String status;

        @XmlElement(name = "AddtlInf")
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