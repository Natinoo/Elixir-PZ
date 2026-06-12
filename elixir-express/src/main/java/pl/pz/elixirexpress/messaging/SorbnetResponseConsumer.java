package pl.pz.elixirexpress.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import pl.pz.elixirexpress.model.PaymentStatus;
import pl.pz.elixirexpress.service.ExpressPaymentService;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Component
public class SorbnetResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(SorbnetResponseConsumer.class);

    private final ExpressPaymentService paymentService;

    public SorbnetResponseConsumer(ExpressPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "responses.elixir-express", groupId = "elixir-express-group")
    public void consume(String message) {
        log.info(">>> ELIXIR-EXPRESS response from SORBNET: {}", message);

        try {
            SorbnetResponse response = parseResponse(message);
            if (response.paymentId == null || response.paymentId.isBlank()) {
                log.warn("Ignoring SORBNET response without paymentId: {}", message);
                return;
            }

            PaymentStatus status = mapStatus(response.status);
            paymentService.updatePaymentStatus(response.paymentId, status);
            log.info("Updated EXPRESS payment {} to status {}", response.paymentId, status);
        } catch (Exception e) {
            log.error("Failed to process Sorbnet response: {}", message, e);
        }
    }

    private SorbnetResponse parseResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        SorbnetResponse response = new SorbnetResponse();
        response.paymentId = firstNonBlank(
                tag(document, "paymentId"),
                tag(document, "PaymentId"),
                tag(document, "OrgnlTxId"),
                tag(document, "OrgnlInstrId")
        );
        response.status = firstNonBlank(tag(document, "status"), tag(document, "Status"), tag(document, "TxSts"));
        response.message = firstNonBlank(tag(document, "message"), tag(document, "Message"), tag(document, "AddtlInf"));
        return response;
    }

    private PaymentStatus mapStatus(String sorbnetStatus) {
        if (sorbnetStatus == null || sorbnetStatus.isBlank()) {
            return PaymentStatus.BLOCKED;
        }
        return switch (sorbnetStatus.trim().toUpperCase()) {
            case "SETTLED", "ACSC", "ACCP", "ACSP" -> PaymentStatus.PROCESSED;
            case "REJECTED", "RJCT" -> PaymentStatus.REJECTED;
            case "BLOCKED", "GRIDLOCK_HELD", "BLCK", "PDNG" -> PaymentStatus.BLOCKED;
            default -> PaymentStatus.QUEUED;
        };
    }

    private String tag(Document document, String tagName) {
        if (document.getElementsByTagName(tagName).getLength() == 0) {
            return null;
        }
        String value = document.getElementsByTagName(tagName).item(0).getTextContent();
        return value == null ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static class SorbnetResponse {
        private String paymentId;
        private String status;
        private String message;
    }
}