package pl.pz.elixirexpress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.service.ExpressPaymentService;

@Component
public class SettlementConsumer {

    private static final Logger log = LoggerFactory.getLogger(SettlementConsumer.class);
    private final ExpressPaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SettlementConsumer(ExpressPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "payments.elixir-express", groupId = "elixir-express-group")
    public void consumeSettlement(String message) {
        log.info("Received settlement from Sorbnet: {}", message);
        try {
            // Zakładamy, że Sorbnet wysyła JSON z polami paymentId i status
            JsonNode json = objectMapper.readTree(message);
            String paymentId = json.get("paymentId").asText();
            String statusStr = json.get("status").asText();
            PaymentStatus status = PaymentStatus.valueOf(statusStr);
            paymentService.updatePaymentStatus(paymentId, status);
        } catch (Exception e) {
            log.error("Failed to process settlement message: {}", message, e);
        }
    }
}