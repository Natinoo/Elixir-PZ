package pl.pz.elixir.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.time.LocalDateTime;

@XmlRootElement(name = "Document")
@XmlAccessorType(XmlAccessType.FIELD)
public class Iso20022AcceptedResponseDto {

    @XmlElement(name = "CstmrPmtStsRpt")
    private CustomerPaymentStatusReport report = new CustomerPaymentStatusReport();

    public Iso20022AcceptedResponseDto() {
    }

    public Iso20022AcceptedResponseDto(String paymentId, String status, String message) {
        this.report.grpHdr.msgId = "RESP-" + paymentId;
        this.report.grpHdr.creDtTm = LocalDateTime.now().toString();
        this.report.orgnlPmtInfAndSts.orgnlPmtInfId = paymentId;
        this.report.orgnlPmtInfAndSts.txInfAndSts.orgnlInstrId = paymentId;
        this.report.orgnlPmtInfAndSts.txInfAndSts.orgnlTxId = paymentId;
        this.report.orgnlPmtInfAndSts.txInfAndSts.txSts = status;
        this.report.orgnlPmtInfAndSts.txInfAndSts.stsRsnInf.addtlInf = message;
    }

    public CustomerPaymentStatusReport getReport() {
        return report;
    }

    public void setReport(CustomerPaymentStatusReport report) {
        this.report = report;
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
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class StatusReasonInformation {
        @XmlElement(name = "AddtlInf")
        private String addtlInf;
    }
}


