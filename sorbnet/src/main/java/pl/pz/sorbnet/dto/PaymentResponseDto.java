package pl.pz.sorbnet.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Odpowiedź SORBNET-u w formacie ISO 20022 (pain.002-style Customer Payment Status Report).
 * Root: <Document><CstmrPmtStsRpt>...
 *
 * UWAGA: struktura tego XML-a musi być 1:1 zgodna z klasą
 * pl.pz.elixir.dto.SorbnetPaymentResponseDto po stronie ELIXIR-a,
 * bo to nią ELIXIR/EXPRESS parsują odpowiedzi z topiców responses.*.
 *
 * Fasadowe settery/gettery zachowują kontrakt starego płaskiego
 * PaymentResponseDto, więc konsumer i kontroler wymagają minimalnych zmian.
 */
@XmlRootElement(name = "Document")
@XmlAccessorType(XmlAccessType.NONE)
public class PaymentResponseDto {

    @XmlElement(name = "CstmrPmtStsRpt")
    private CustomerPaymentStatusReport report = new CustomerPaymentStatusReport();

    public PaymentResponseDto() {
    }

    // ===== fasada: paymentId =====

    public String getPaymentId() {
        TransactionInformationAndStatus tx = tx();
        if (tx.orgnlTxId != null && !tx.orgnlTxId.isBlank()) {
            return tx.orgnlTxId;
        }
        return tx.orgnlInstrId;
    }

    public void setPaymentId(String paymentId) {
        report().orgnlPmtInfAndSts.orgnlPmtInfId = paymentId;
        tx().orgnlInstrId = paymentId;
        tx().orgnlTxId = paymentId;
        report().grpHdr.msgId = "RESP-" + paymentId;
        if (report().grpHdr.creDtTm == null) {
            report().grpHdr.creDtTm = LocalDateTime.now().toString();
        }
    }

    // ===== fasada: status / message =====

    public String getStatus() {
        return tx().txSts;
    }

    public void setStatus(String status) {
        tx().txSts = status;
    }

    public String getMessage() {
        return tx().stsRsnInf.addtlInf;
    }

    public void setMessage(String message) {
        tx().stsRsnInf.addtlInf = message;
    }

    // ===== fasada: banki i rachunki =====

    public String getSenderBankId() {
        return tx().orgnlTxRef.dbtrAgt.finInstnId.bicfi;
    }

    public void setSenderBankId(String senderBankId) {
        tx().orgnlTxRef.dbtrAgt.finInstnId.bicfi = senderBankId;
    }

    public String getReceiverBankId() {
        return tx().orgnlTxRef.cdtrAgt.finInstnId.bicfi;
    }

    public void setReceiverBankId(String receiverBankId) {
        tx().orgnlTxRef.cdtrAgt.finInstnId.bicfi = receiverBankId;
    }

    public String getSenderAccount() {
        return tx().orgnlTxRef.dbtrAcct.id.iban;
    }

    public void setSenderAccount(String senderAccount) {
        tx().orgnlTxRef.dbtrAcct.id.iban = senderAccount;
    }

    public String getReceiverAccount() {
        return tx().orgnlTxRef.cdtrAcct.id.iban;
    }

    public void setReceiverAccount(String receiverAccount) {
        tx().orgnlTxRef.cdtrAcct.id.iban = receiverAccount;
    }

    // ===== fasada: kwota =====

    public BigDecimal getAmount() {
        return tx().orgnlTxRef.intrBkSttlmAmt == null ? null : tx().orgnlTxRef.intrBkSttlmAmt.value;
    }

    public void setAmount(BigDecimal amount) {
        if (tx().orgnlTxRef.intrBkSttlmAmt == null) {
            tx().orgnlTxRef.intrBkSttlmAmt = new ActiveCurrencyAndAmount();
        }
        tx().orgnlTxRef.intrBkSttlmAmt.value = amount;
        if (tx().orgnlTxRef.intrBkSttlmAmt.currency == null) {
            tx().orgnlTxRef.intrBkSttlmAmt.currency = "PLN";
        }
    }

    public void setCurrency(String currency) {
        if (tx().orgnlTxRef.intrBkSttlmAmt == null) {
            tx().orgnlTxRef.intrBkSttlmAmt = new ActiveCurrencyAndAmount();
        }
        tx().orgnlTxRef.intrBkSttlmAmt.currency = currency;
    }

    // ===== fasada: settledAt =====

    public String getSettledAt() {
        return tx().settledAt;
    }

    public void setSettledAt(String settledAt) {
        tx().settledAt = settledAt;
    }

    // ===== fasada: serwis źródłowy (ELIXIR / ELIXIR_EXPRESS / SORBNET) =====

    public String getSourceServiceCode() {
        return tx().orgnlTxRef.splmtryData.envlp.sourceServiceCode;
    }

    public void setSourceServiceCode(String sourceServiceCode) {
        tx().orgnlTxRef.splmtryData.envlp.sourceServiceCode = sourceServiceCode;
    }

    // ===== nawigacja po drzewie =====

    private CustomerPaymentStatusReport report() {
        if (report == null) {
            report = new CustomerPaymentStatusReport();
        }
        if (report.grpHdr == null) {
            report.grpHdr = new GroupHeader();
        }
        if (report.orgnlPmtInfAndSts == null) {
            report.orgnlPmtInfAndSts = new OriginalPaymentInformationAndStatus();
        }
        return report;
    }

    private TransactionInformationAndStatus tx() {
        CustomerPaymentStatusReport r = report();
        if (r.orgnlPmtInfAndSts.txInfAndSts == null) {
            r.orgnlPmtInfAndSts.txInfAndSts = new TransactionInformationAndStatus();
        }
        return r.orgnlPmtInfAndSts.txInfAndSts;
    }

    // ===== struktura XML =====

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CustomerPaymentStatusReport {
        @XmlElement(name = "GrpHdr")
        private GroupHeader grpHdr = new GroupHeader();

        @XmlElement(name = "OrgnlPmtInfAndSts")
        private OriginalPaymentInformationAndStatus orgnlPmtInfAndSts = new OriginalPaymentInformationAndStatus();
    }
    @XmlType(name = "ResponseGroupHeader")
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
    @XmlType(name = "ResponseActiveCurrencyAndAmount")      
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ActiveCurrencyAndAmount {
        @XmlAttribute(name = "Ccy")
        private String currency;

        @XmlValue
        private BigDecimal value;
    }
    @XmlType(name = "ResponseBranchAndFinancialInstitutionIdentification")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BranchAndFinancialInstitutionIdentification {
        @XmlElement(name = "FinInstnId")
        private FinancialInstitutionIdentification finInstnId = new FinancialInstitutionIdentification();
    }
    @XmlType(name = "ResponseFinancialInstitutionIdentification")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FinancialInstitutionIdentification {
        @XmlElement(name = "BICFI")
        private String bicfi;
    }
    @XmlType(name = "ResponseCashAccount")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CashAccount {
        @XmlElement(name = "Id")
        private AccountIdentification id = new AccountIdentification();
    }
    @XmlType(name = "ResponseAccountIdentification")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AccountIdentification {
        @XmlElement(name = "IBAN")
        private String iban;
    }
    @XmlType(name = "ResponseSupplementaryData") 
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SupplementaryData {
        @XmlElement(name = "Envlp")
        private Envelope envlp = new Envelope();
    }
    @XmlType(name = "ResponseEnvelope")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Envelope {
        @XmlElement(name = "SourceServiceCode")
        private String sourceServiceCode;
    }
}