package pl.pz.elixir.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@XmlRootElement(name = "Document")
@XmlAccessorType(XmlAccessType.FIELD)
public class LiquidityTransferRequestDto {

    @XmlElement(name = "LiquidityCreditTransferRequest")
    private LiquidityCreditTransferRequest request = new LiquidityCreditTransferRequest();

    public LiquidityTransferRequestDto() {
    }

    public LiquidityTransferRequestDto(String requestId, String sessionId, String bankId,
                                       String sourceServiceCode, String targetServiceCode,
                                       String sourceAccount, String targetAccount,
                                       BigDecimal amount, String currency, String reason,
                                       boolean sourceHasFunds) {
        request.grpHdr.msgId = requestId;
        request.grpHdr.creDtTm = LocalDateTime.now().toString();
        request.trfInstr.requestId = requestId;
        request.trfInstr.sessionId = sessionId;
        request.trfInstr.bankId = bankId;
        request.trfInstr.sourceServiceCode = sourceServiceCode;
        request.trfInstr.targetServiceCode = targetServiceCode;
        request.trfInstr.sourceAccount = sourceAccount;
        request.trfInstr.targetAccount = targetAccount;
        request.trfInstr.amount = new ActiveCurrencyAndAmount(currency, amount);
        request.trfInstr.reason = reason;
        request.trfInstr.sourceHasFunds = sourceHasFunds;
        request.trfInstr.approvalStatus = "PENDING_BANK_APPROVAL";
    }

    public String getRequestId() {
        return request.trfInstr.requestId;
    }

    public String getSessionId() {
        return request.trfInstr.sessionId;
    }

    public String getBankId() {
        return request.trfInstr.bankId;
    }

    public String getSourceServiceCode() {
        return request.trfInstr.sourceServiceCode;
    }

    public String getTargetServiceCode() {
        return request.trfInstr.targetServiceCode;
    }

    public String getSourceAccount() {
        return request.trfInstr.sourceAccount;
    }

    public String getTargetAccount() {
        return request.trfInstr.targetAccount;
    }

    public BigDecimal getAmount() {
        return request.trfInstr.amount == null ? null : request.trfInstr.amount.value;
    }

    public String getCurrency() {
        return request.trfInstr.amount == null ? null : request.trfInstr.amount.currency;
    }

    public String getReason() {
        return request.trfInstr.reason;
    }

    public boolean isSourceHasFunds() {
        return request.trfInstr.sourceHasFunds;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LiquidityCreditTransferRequest {

        @XmlElement(name = "GrpHdr")
        private GroupHeader grpHdr = new GroupHeader();

        @XmlElement(name = "TrfInstr")
        private TransferInstruction trfInstr = new TransferInstruction();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GroupHeader {

        @XmlElement(name = "MsgId")
        private String msgId;

        @XmlElement(name = "CreDtTm")
        private String creDtTm;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TransferInstruction {

        @XmlElement(name = "ReqId")
        private String requestId;

        @XmlElement(name = "SessionId")
        private String sessionId;

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
        private String reason;

        @XmlElement(name = "SourceHasFunds")
        private boolean sourceHasFunds;

        @XmlElement(name = "ApprovalStatus")
        private String approvalStatus;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ActiveCurrencyAndAmount {

        @XmlAttribute(name = "Ccy")
        private String currency;

        @XmlValue
        private BigDecimal value;

        public ActiveCurrencyAndAmount() {
        }

        public ActiveCurrencyAndAmount(String currency, BigDecimal value) {
            this.currency = currency;
            this.value = value;
        }
    }
}