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
public class NettingSettlementInstructionDto {

    @XmlElement(name = "FICdtTrf")
    private FinancialInstitutionCreditTransfer fiCdtTrf = new FinancialInstitutionCreditTransfer();

    public NettingSettlementInstructionDto() {
    }

    public NettingSettlementInstructionDto(NettingTransferDto transfer) {
        setTransferId(transfer.getTransferId());
        setSessionId(transfer.getSessionId());
        setDebtorBankId(transfer.getDebtorBankId());
        setCreditorBankId(transfer.getCreditorBankId());
        setDebtorAccount(transfer.getDebtorAccount());
        setCreditorAccount(transfer.getCreditorAccount());
        setAmount(transfer.getAmount());
        setCurrency(transfer.getCurrency());
        setServiceCode(transfer.getServiceCode());
        fiCdtTrf.grpHdr.msgId = transfer.getTransferId();
        fiCdtTrf.grpHdr.creDtTm = LocalDateTime.now().toString();
        fiCdtTrf.grpHdr.nbOfTxs = "1";
        fiCdtTrf.grpHdr.sttlmInf.sttlmMtd = "CLRG";
        fiCdtTrf.grpHdr.sttlmInf.clrSys.cd = "SORBNET";
    }

    public String getTransferId() {
        return tx().pmtId.txId;
    }

    public void setTransferId(String transferId) {
        tx().pmtId.instrId = transferId;
        tx().pmtId.endToEndId = transferId;
        tx().pmtId.txId = transferId;
    }

    public String getSessionId() {
        return tx().splmtryData.envlp.sessionId;
    }

    public void setSessionId(String sessionId) {
        tx().splmtryData.envlp.sessionId = sessionId;
    }

    public String getDebtorBankId() {
        return tx().dbtrAgt.finInstnId.bicfi;
    }

    public void setDebtorBankId(String bankId) {
        tx().dbtrAgt.finInstnId.bicfi = bankId;
        tx().splmtryData.envlp.debtorBankId = bankId;
    }

    public String getCreditorBankId() {
        return tx().cdtrAgt.finInstnId.bicfi;
    }

    public void setCreditorBankId(String bankId) {
        tx().cdtrAgt.finInstnId.bicfi = bankId;
        tx().splmtryData.envlp.creditorBankId = bankId;
    }

    public String getDebtorAccount() {
        return tx().dbtrAcct.id.iban;
    }

    public void setDebtorAccount(String account) {
        tx().dbtrAcct.id.iban = account;
    }

    public String getCreditorAccount() {
        return tx().cdtrAcct.id.iban;
    }

    public void setCreditorAccount(String account) {
        tx().cdtrAcct.id.iban = account;
    }

    public BigDecimal getAmount() {
        return tx().intrBkSttlmAmt == null ? null : tx().intrBkSttlmAmt.value;
    }

    public void setAmount(BigDecimal amount) {
        tx().intrBkSttlmAmt.value = amount;
        fiCdtTrf.grpHdr.ttlIntrBkSttlmAmt.value = amount;
    }

    public String getCurrency() {
        return tx().intrBkSttlmAmt == null ? null : tx().intrBkSttlmAmt.currency;
    }

    public void setCurrency(String currency) {
        tx().intrBkSttlmAmt.currency = currency;
        fiCdtTrf.grpHdr.ttlIntrBkSttlmAmt.currency = currency;
    }

    public String getServiceCode() {
        return tx().splmtryData.envlp.sourceServiceCode;
    }

    public void setServiceCode(String serviceCode) {
        tx().splmtryData.envlp.sourceServiceCode = serviceCode;
    }

    private CreditTransferTransactionInformation tx() {
        if (fiCdtTrf == null) {
            fiCdtTrf = new FinancialInstitutionCreditTransfer();
        }
        if (fiCdtTrf.cdtTrfTxInf == null) {
            fiCdtTrf.cdtTrfTxInf = new CreditTransferTransactionInformation();
        }
        return fiCdtTrf.cdtTrfTxInf;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FinancialInstitutionCreditTransfer {
        @XmlElement(name = "GrpHdr")
        private GroupHeader grpHdr = new GroupHeader();

        @XmlElement(name = "CdtTrfTxInf")
        private CreditTransferTransactionInformation cdtTrfTxInf = new CreditTransferTransactionInformation();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GroupHeader {
        @XmlElement(name = "MsgId")
        private String msgId;

        @XmlElement(name = "CreDtTm")
        private String creDtTm;

        @XmlElement(name = "NbOfTxs")
        private String nbOfTxs = "1";

        @XmlElement(name = "TtlIntrBkSttlmAmt")
        private ActiveCurrencyAndAmount ttlIntrBkSttlmAmt = new ActiveCurrencyAndAmount();

        @XmlElement(name = "SttlmInf")
        private SettlementInformation sttlmInf = new SettlementInformation();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SettlementInformation {
        @XmlElement(name = "SttlmMtd")
        private String sttlmMtd = "CLRG";

        @XmlElement(name = "ClrSys")
        private ClearingSystemIdentification clrSys = new ClearingSystemIdentification();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ClearingSystemIdentification {
        @XmlElement(name = "Cd")
        private String cd = "SORBNET";
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CreditTransferTransactionInformation {
        @XmlElement(name = "PmtId")
        private PaymentIdentification pmtId = new PaymentIdentification();

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

        @XmlElement(name = "RmtInf")
        private RemittanceInformation rmtInf = new RemittanceInformation();

        @XmlElement(name = "SplmtryData")
        private SupplementaryData splmtryData = new SupplementaryData();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PaymentIdentification {
        @XmlElement(name = "InstrId")
        private String instrId;

        @XmlElement(name = "EndToEndId")
        private String endToEndId;

        @XmlElement(name = "TxId")
        private String txId;
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
    public static class RemittanceInformation {
        @XmlElement(name = "Ustrd")
        private String ustrd = "Netting settlement";
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SupplementaryData {
        @XmlElement(name = "Envlp")
        private Envelope envlp = new Envelope();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Envelope {
        @XmlElement(name = "SourceServiceCode")
        private String sourceServiceCode = "ELIXIR";

        @XmlElement(name = "SessionId")
        private String sessionId;

        @XmlElement(name = "DebtorBankId")
        private String debtorBankId;

        @XmlElement(name = "CreditorBankId")
        private String creditorBankId;
    }
}
