package pl.pz.elixirexpress;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.service.ExpressPaymentService;

import java.io.StringReader;

@Component
public class SettlementConsumer {

    private static final Logger log = LoggerFactory.getLogger(SettlementConsumer.class);
    private final ExpressPaymentService paymentService;
    private final JAXBContext jaxbContext;

    public SettlementConsumer(ExpressPaymentService paymentService) throws Exception {
        this.paymentService = paymentService;
        this.jaxbContext = JAXBContext.newInstance(SorbnetPaymentResponse.class);
    }

    @KafkaListener(topics = "responses.elixir-express", groupId = "elixir-express-group")
    public void consumeSettlement(String message) {
        log.info("=== SETTLEMENT CONSUMER RECEIVED MESSAGE ===");
        log.info("Raw message: {}", message);
        
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            SorbnetPaymentResponse response = (SorbnetPaymentResponse) unmarshaller.unmarshal(new StringReader(message));
            
            String paymentId = response.getPaymentId();
            String statusStr = response.getStatus();
            
            log.info("Parsed: paymentId={}, status={}", paymentId, statusStr);
            
            // Mapowanie statusu z Sorbnet na PaymentStatus
            PaymentStatus status = mapStatus(statusStr);
            paymentService.updatePaymentStatus(paymentId, status);
            
            log.info("Updated payment {} to status {}", paymentId, status);
        } catch (Exception e) {
            log.error("Failed to process settlement message: {}", message, e);
        }
    }
    
    private PaymentStatus mapStatus(String sorbnetStatus) {
        switch (sorbnetStatus) {
            case "SETTLED":
                return PaymentStatus.PROCESSED;
            case "REJECTED":
                return PaymentStatus.REJECTED;
            case "BLOCKED", "GRIDLOCK_HELD":
                return PaymentStatus.BLOCKED;
            default:
                log.warn("Unknown status: {}, defaulting to QUEUED", sorbnetStatus);
                return PaymentStatus.QUEUED;
        }
    }
    
    @XmlRootElement(name = "SorbnetPaymentResponse")
    public static class SorbnetPaymentResponse {
        private String paymentId;
        private String status;
        private String message;
        private String senderBankId;
        private String receiverBankId;
        private Double amount;
        private String settledAt;
        
        @XmlElement(name = "paymentId")
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        
        @XmlElement(name = "status")
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        @XmlElement(name = "message")
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        @XmlElement(name = "senderBankId")
        public String getSenderBankId() { return senderBankId; }
        public void setSenderBankId(String senderBankId) { this.senderBankId = senderBankId; }
        
        @XmlElement(name = "receiverBankId")
        public String getReceiverBankId() { return receiverBankId; }
        public void setReceiverBankId(String receiverBankId) { this.receiverBankId = receiverBankId; }
        
        @XmlElement(name = "amount")
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        @XmlElement(name = "settledAt")
        public String getSettledAt() { return settledAt; }
        public void setSettledAt(String settledAt) { this.settledAt = settledAt; }
    }
}
