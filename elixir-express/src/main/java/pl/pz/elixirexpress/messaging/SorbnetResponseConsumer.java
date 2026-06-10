package pl.pz.elixirexpress.messaging;

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
public class SorbnetResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(SorbnetResponseConsumer.class);

    private final ExpressPaymentService paymentService;
    private final JAXBContext jaxbContext;

    public SorbnetResponseConsumer(ExpressPaymentService paymentService) throws Exception {
        this.paymentService = paymentService;
        this.jaxbContext = JAXBContext.newInstance(SorbnetPaymentResponse.class);
    }

    @KafkaListener(topics = "responses.elixir-express", groupId = "elixir-express-group")
    public void consume(String message) {
        log.info(">>> ELIXIR-EXPRESS response from SORBNET: {}", message);

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            SorbnetPaymentResponse response = (SorbnetPaymentResponse) unmarshaller.unmarshal(new StringReader(message));

            String paymentId = response.getPaymentId();
            String statusStr = response.getStatus();

            log.info("Parsed: paymentId={}, status={}", paymentId, statusStr);

            PaymentStatus status = switch (statusStr) {
                case "SETTLED" -> PaymentStatus.PROCESSED;
                case "REJECTED" -> PaymentStatus.REJECTED;
                case "BLOCKED", "GRIDLOCK_HELD" -> PaymentStatus.BLOCKED;
                default -> PaymentStatus.QUEUED;
            };

            paymentService.updatePaymentStatus(paymentId, status);
            log.info("Updated payment {} to status {}", paymentId, status);

        } catch (Exception e) {
            log.error("Failed to process Sorbnet response: {}", message, e);
        }
    }

    @XmlRootElement(name = "SorbnetPaymentResponse")
    public static class SorbnetPaymentResponse {
        private String paymentId;
        private String status;

        @XmlElement(name = "paymentId")
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

        @XmlElement(name = "status")
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}