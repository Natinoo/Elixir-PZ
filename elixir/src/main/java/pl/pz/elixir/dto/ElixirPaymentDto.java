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
@XmlAccessorType(XmlAccessType.NONE)
public class ElixirPaymentDto {

    @XmlElement(name = "FIToFICstmrCdtTrf")
    private FIToFICustomerCreditTransfer fiToFICstmrCdtTrf = new FIToFICustomerCreditTransfer();

    public ElixirPaymentDto() {
    }

    public ElixirPaymentDto(String paymentId, BigDecimal amount, String currency, String senderBankId,
                            String receiverBankId, String senderAccount, String receiverAccount,
                            String title, String type) {
        setPaymentId(paymentId);
        setAmount(amount);
        setCurrency(currency);
        setSenderBankId(senderBankId);
        setReceiverBankId(receiverBankId);
        setSenderAccount(senderAccount);
        setReceiverAccount(receiverAccount);
        setTitle(title);
        setType(type);
        ensureDefaults();
    }

    public ElixirPaymentDto(String paymentId, BigDecimal amount, String currency, String senderBankId,
                            String receiverBankId, String senderAccount, String receiverAccount,
                            String senderName, String receiverName, String title, String type) {
        setPaymentId(paymentId);
        setAmount(amount);
        setCurrency(currency);
        setSenderBankId(senderBankId);
        setReceiverBankId(receiverBankId);
        setSenderAccount(senderAccount);
        setReceiverAccount(receiverAccount);
        setSenderName(senderName);
        setReceiverName(receiverName);
        setTitle(title);
        setType(type);
        ensureDefaults();
    }

    public void ensureDefaults() {
        if (getPaymentId() == null || getPaymentId().isBlank()) {
            setPaymentId("ELIX-" + System.currentTimeMillis());
        }

        if (getType() == null || getType().isBlank()) {
            setType("ELIXIR");
        }

        if (grpHdr().msgId == null || grpHdr().msgId.isBlank()) {
            grpHdr().msgId = getPaymentId();
        }

        if (grpHdr().creDtTm == null || grpHdr().creDtTm.isBlank()) {
            grpHdr().creDtTm = LocalDateTime.now().toString();
        }

        grpHdr().nbOfTxs = "1";
        grpHdr().ttlIntrBkSttlmAmt = new ActiveCurrencyAndAmount(getCurrency(), getAmount());
        grpHdr().sttlmInf.sttlmMtd = "CLRG";
        grpHdr().sttlmInf.clrSys.cd = getType();
    }

    public String getPaymentId() {
        PaymentIdentification pmtId = tx().pmtId;

        if (pmtId.txId != null && !pmtId.txId.isBlank()) {
            return pmtId.txId;
        }

        if (pmtId.instrId != null && !pmtId.instrId.isBlank()) {
            return pmtId.instrId;
        }

        return pmtId.endToEndId;
    }

    public void setPaymentId(String paymentId) {
        tx().pmtId.instrId = paymentId;
        tx().pmtId.endToEndId = paymentId;
        tx().pmtId.txId = paymentId;
        grpHdr().msgId = paymentId;
    }

    public BigDecimal getAmount() {
        return tx().intrBkSttlmAmt == null ? null : tx().intrBkSttlmAmt.value;
    }

    public void setAmount(BigDecimal amount) {
        if (tx().intrBkSttlmAmt == null) {
            tx().intrBkSttlmAmt = new ActiveCurrencyAndAmount();
        }

        tx().intrBkSttlmAmt.value = amount;

        if (grpHdr().ttlIntrBkSttlmAmt == null) {
            grpHdr().ttlIntrBkSttlmAmt = new ActiveCurrencyAndAmount();
        }

        grpHdr().ttlIntrBkSttlmAmt.value = amount;
    }

    public void setAmount(Double amount) {
        setAmount(amount == null ? null : BigDecimal.valueOf(amount));
    }

    public String getCurrency() {
        return tx().intrBkSttlmAmt == null ? null : tx().intrBkSttlmAmt.currency;
    }

    public void setCurrency(String currency) {
        if (tx().intrBkSttlmAmt == null) {
            tx().intrBkSttlmAmt = new ActiveCurrencyAndAmount();
        }

        tx().intrBkSttlmAmt.currency = currency;

        if (grpHdr().ttlIntrBkSttlmAmt == null) {
            grpHdr().ttlIntrBkSttlmAmt = new ActiveCurrencyAndAmount();
        }

        grpHdr().ttlIntrBkSttlmAmt.currency = currency;
    }

    public String getSenderBankId() {
        return tx().dbtrAgt.finInstnId.bicfi;
    }

    public void setSenderBankId(String senderBankId) {
        tx().dbtrAgt.finInstnId.bicfi = senderBankId;
        tx().splmtryData.envlp.senderBankId = senderBankId;
    }

    public String getReceiverBankId() {
        return tx().cdtrAgt.finInstnId.bicfi;
    }

    public void setReceiverBankId(String receiverBankId) {
        tx().cdtrAgt.finInstnId.bicfi = receiverBankId;
        tx().splmtryData.envlp.receiverBankId = receiverBankId;
    }

    public String getSenderAccount() {
        return tx().dbtrAcct.id.iban;
    }

    public void setSenderAccount(String senderAccount) {
        tx().dbtrAcct.id.iban = senderAccount;
    }

    public String getReceiverAccount() {
        return tx().cdtrAcct.id.iban;
    }

    public void setReceiverAccount(String receiverAccount) {
        tx().cdtrAcct.id.iban = receiverAccount;
    }

    public String getSenderName() {
        return tx().dbtr.nm;
    }

    public void setSenderName(String senderName) {
        tx().dbtr.nm = senderName;
    }

    public String getReceiverName() {
        return tx().cdtr.nm;
    }

    public void setReceiverName(String receiverName) {
        tx().cdtr.nm = receiverName;
    }

    public String getTitle() {
        return tx().rmtInf.ustrd;
    }

    public void setTitle(String title) {
        tx().rmtInf.ustrd = title;
    }

    public String getType() {
        return tx().splmtryData.envlp.serviceCode;
    }

    public void setType(String type) {
        tx().splmtryData.envlp.serviceCode = type;
        grpHdr().sttlmInf.clrSys.cd = type;
    }

    public FIToFICustomerCreditTransfer getFiToFICstmrCdtTrf() {
        return fiToFICstmrCdtTrf;
    }

    public void setFiToFICstmrCdtTrf(FIToFICustomerCreditTransfer fiToFICstmrCdtTrf) {
        this.fiToFICstmrCdtTrf = fiToFICstmrCdtTrf;
    }

    private GroupHeader grpHdr() {
        if (fiToFICstmrCdtTrf == null) {
            fiToFICstmrCdtTrf = new FIToFICustomerCreditTransfer();
        }

        if (fiToFICstmrCdtTrf.grpHdr == null) {
            fiToFICstmrCdtTrf.grpHdr = new GroupHeader();
        }

        return fiToFICstmrCdtTrf.grpHdr;
    }

    private CreditTransferTransactionInformation tx() {
        if (fiToFICstmrCdtTrf == null) {
            fiToFICstmrCdtTrf = new FIToFICustomerCreditTransfer();
        }

        if (fiToFICstmrCdtTrf.cdtTrfTxInf == null) {
            fiToFICstmrCdtTrf.cdtTrfTxInf = new CreditTransferTransactionInformation();
        }

        return fiToFICstmrCdtTrf.cdtTrfTxInf;
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class FIToFICustomerCreditTransfer {

        @XmlElement(name = "GrpHdr")
        private GroupHeader grpHdr = new GroupHeader();

        @XmlElement(name = "CdtTrfTxInf")
        private CreditTransferTransactionInformation cdtTrfTxInf = new CreditTransferTransactionInformation();
    }

    @XmlAccessorType(XmlAccessType.NONE)
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

    @XmlAccessorType(XmlAccessType.NONE)
    public static class SettlementInformation {

        @XmlElement(name = "SttlmMtd")
        private String sttlmMtd = "CLRG";

        @XmlElement(name = "ClrSys")
        private ClearingSystemIdentification clrSys = new ClearingSystemIdentification();
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class ClearingSystemIdentification {

        @XmlElement(name = "Cd")
        private String cd = "ELIXIR";
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class CreditTransferTransactionInformation {

        @XmlElement(name = "PmtId")
        private PaymentIdentification pmtId = new PaymentIdentification();

        @XmlElement(name = "IntrBkSttlmAmt")
        private ActiveCurrencyAndAmount intrBkSttlmAmt = new ActiveCurrencyAndAmount();

        @XmlElement(name = "Dbtr")
        private PartyIdentification dbtr = new PartyIdentification();

        @XmlElement(name = "DbtrAcct")
        private CashAccount dbtrAcct = new CashAccount();

        @XmlElement(name = "DbtrAgt")
        private BranchAndFinancialInstitutionIdentification dbtrAgt =
                new BranchAndFinancialInstitutionIdentification();

        @XmlElement(name = "Cdtr")
        private PartyIdentification cdtr = new PartyIdentification();

        @XmlElement(name = "CdtrAcct")
        private CashAccount cdtrAcct = new CashAccount();

        @XmlElement(name = "CdtrAgt")
        private BranchAndFinancialInstitutionIdentification cdtrAgt =
                new BranchAndFinancialInstitutionIdentification();

        @XmlElement(name = "RmtInf")
        private RemittanceInformation rmtInf = new RemittanceInformation();

        @XmlElement(name = "SplmtryData")
        private SupplementaryData splmtryData = new SupplementaryData();
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class PaymentIdentification {

        @XmlElement(name = "InstrId")
        private String instrId;

        @XmlElement(name = "EndToEndId")
        private String endToEndId;

        @XmlElement(name = "TxId")
        private String txId;
    }

    @XmlAccessorType(XmlAccessType.NONE)
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

    @XmlAccessorType(XmlAccessType.NONE)
    public static class PartyIdentification {

        @XmlElement(name = "Nm")
        private String nm;
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class BranchAndFinancialInstitutionIdentification {

        @XmlElement(name = "FinInstnId")
        private FinancialInstitutionIdentification finInstnId =
                new FinancialInstitutionIdentification();
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class FinancialInstitutionIdentification {

        @XmlElement(name = "BICFI")
        private String bicfi;
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class CashAccount {

        @XmlElement(name = "Id")
        private AccountIdentification id = new AccountIdentification();
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class AccountIdentification {

        @XmlElement(name = "IBAN")
        private String iban;
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class RemittanceInformation {

        @XmlElement(name = "Ustrd")
        private String ustrd;
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class SupplementaryData {

        @XmlElement(name = "Envlp")
        private Envelope envlp = new Envelope();
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class Envelope {

        @XmlElement(name = "ServiceCode")
        private String serviceCode = "ELIXIR";

        @XmlElement(name = "SenderBankId")
        private String senderBankId;

        @XmlElement(name = "ReceiverBankId")
        private String receiverBankId;
    }
}