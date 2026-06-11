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
public class SorbnetPaymentResponseDto {

    @XmlElement(name = "CstmrPmtStsRpt")
    private CustomerPaymentStatusReport report = new CustomerPaymentStatusReport();

    public SorbnetPaymentResponseDto() {
    }

    public String getPaymentId() {
        TransactionInformationAndStatus tx = tx();
        if (tx.orgnlTxId != null && !tx.orgnlTxId.isBlank()) {
            return tx.orgnlTxId;
        }
        return tx.orgnlInstrId;
    }

    public String getStatus() {
        return tx().txSts;
    }

    public String getMessage() {
        return tx().stsRsnInf.addtlInf;
    }

    public String getSenderBankId() {
        return tx().orgnlTxRef.dbtrAgt.finInstnId.bicfi;
    }

    public String getReceiverBankId() {
        return tx().orgnlTxRef.cdtrAgt.finInstnId.bicfi;
    }

    public String getSenderAccount() {
        return tx().orgnlTxRef.dbtrAcct.id.iban;
    }

    public String getReceiverAccount() {
        return tx().orgnlTxRef.cdtrAcct.id.iban;
    }

    public BigDecimal getAmount() {
        return tx().orgnlTxRef.intrBkSttlmAmt == null ? null : tx().orgnlTxRef.intrBkSttlmAmt.value;
    }

    public String getSettledAt() {
        return tx().settledAt;
    }

    public String getType() {
        return tx().orgnlTxRef.splmtryData.envlp.sourceServiceCode;
    }

    private TransactionInformationAndStatus tx() {
        if (report == null) {
            report = new CustomerPaymentStatusReport();
        }
        if (report.orgnlPmtInfAndSts == null) {
            report.orgnlPmtInfAndSts = new OriginalPaymentInformationAndStatus();
        }
        if (report.orgnlPmtInfAndSts.txInfAndSts == null) {
            report.orgnlPmtInfAndSts.txInfAndSts = new TransactionInformationAndStatus();
        }
        return report.orgnlPmtInfAndSts.txInfAndSts;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CustomerPaymentStatusReport {
        @XmlElement(name = "GrpHdr")
        private GroupHeader grpHdr = new GroupHeader();

        @XmlElement(name = "OrgnlPmtInfAndSts")
        private OriginalPaymentInformationAndStatus orgnlPmtInfAndSts = new OriginalPaymentInformationAndStatus();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GroupHeader {
        @XmlElement(name = "MsgId")
        private String msgId;

        @XmlElement(name = "CreDtTm")
        private String creDtTm;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OriginalPaymentInformationAndStatus {
        @XmlElement(name = "OrgnlPmtInfId")
        private String orgnlPmtInfId;

        @XmlElement(name = "TxInfAndSts")
        private TransactionInformationAndStatus txInfAndSts = new TransactionInformationAndStatus();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TransactionInformationAndStatus {
        @XmlElement(name = "OrgnlInstrId")
        private String orgnlInstrId;

        @XmlElement(name = "OrgnlTxId")
        private String orgnlTxId;

        @XmlElement(name = "TxSts")
        private String txSts;

        @XmlElement(name = "StsRsnInf")
        private StatusReasonInformation stsRsnInf = new StatusReasonInformation();

        @XmlElement(name = "OrgnlTxRef")
        private OriginalTransactionReference orgnlTxRef = new OriginalTransactionReference();

        @XmlElement(name = "SettledAt")
        private String settledAt;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class StatusReasonInformation {
        @XmlElement(name = "AddtlInf")
        private String addtlInf;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OriginalTransactionReference {
        @XmlElement(name = "IntrBkSttlmAmt")
        private ActiveCurrencyAndAmount intrBkSttlmAmt = new ActiveCurrencyAndAmount();

        @XmlElement(name = "DbtrAgt")
        private BranchAndFinancialInstitutionIdentification dbtrAgt = new BranchAndFinancialInstitutionIdentification();

        @XmlElement(name = "CdtrAgt")
        private BranchAndFinancialInstitutionIdentification cdtrAgt = new BranchAndFinancialInstitutionIdentification();

        @XmlElement(name = "DbtrAcct")
        private CashAccount dbtrAcct = new CashAccount();

        @XmlElement(name = "CdtrAcct")
        private CashAccount cdtrAcct = new CashAccount();

        @XmlElement(name = "SplmtryData")
        private SupplementaryData splmtryData = new SupplementaryData();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ActiveCurrencyAndAmount {
        @XmlAttribute(name = "Ccy")
        private String currency;

        @XmlValue
        private BigDecimal value;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BranchAndFinancialInstitutionIdentification {
        @XmlElement(name = "FinInstnId")
        private FinancialInstitutionIdentification finInstnId = new FinancialInstitutionIdentification();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FinancialInstitutionIdentification {
        @XmlElement(name = "BICFI")
        private String bicfi;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CashAccount {
        @XmlElement(name = "Id")
        private AccountIdentification id = new AccountIdentification();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AccountIdentification {
        @XmlElement(name = "IBAN")
        private String iban;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SupplementaryData {
        @XmlElement(name = "Envlp")
        private Envelope envlp = new Envelope();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Envelope {
        @XmlElement(name = "SourceServiceCode")
        private String sourceServiceCode;
    }
}
