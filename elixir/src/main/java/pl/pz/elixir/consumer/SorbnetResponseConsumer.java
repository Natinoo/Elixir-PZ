package pl.pz.elixir.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Component
public class SorbnetResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(SorbnetResponseConsumer.class);

    private final ElixirPaymentService elixirPaymentService;

    public SorbnetResponseConsumer(ElixirPaymentService elixirPaymentService) {
        this.elixirPaymentService = elixirPaymentService;
    }

    @KafkaListener(topics = "responses.elixir", groupId = "elixir-group")
    public void consume(String message) {
        try {
            SorbnetResponse response = parseResponse(message);

            if (response.paymentId == null || response.paymentId.isBlank()) {
                log.warn("SORBNet response ignored: missing paymentId. XML={}", message);
                return;
            }

            PaymentStatus mappedStatus = mapStatus(response.status);

            elixirPaymentService.updatePaymentStatus(response.paymentId, mappedStatus);

            log.info(
                    "SORBNet response processed: paymentId={}, sorbnetStatus={}, mappedStatus={}, settledAt={}, message={}",
                    response.paymentId,
                    response.status,
                    mappedStatus,
                    response.settledAt,
                    response.message
            );

        } catch (Exception e) {
            log.error("Cannot process SORBNet XML response: {}", message, e);
        }
    }

    private SorbnetResponse parseResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        Document document = factory
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        SorbnetResponse response = new SorbnetResponse();

        // Stary format:
        // <SorbnetPaymentResponse>
        //   <paymentId>...</paymentId>
        //   <status>SETTLED</status>
        // </SorbnetPaymentResponse>
        response.paymentId = firstNonBlank(
                tag(document, "paymentId"),
                tag(document, "PaymentId"),
                tag(document, "OrgnlTxId"),
                tag(document, "OrgnlInstrId"),
                tag(document, "OrgnlPmtInfId")
        );

        response.status = firstNonBlank(
                tag(document, "status"),
                tag(document, "Status"),
                tag(document, "TxSts")
        );

        response.message = firstNonBlank(
                tag(document, "message"),
                tag(document, "Message"),
                tag(document, "AddtlInf")
        );

        response.settledAt = firstNonBlank(
                tag(document, "settledAt"),
                tag(document, "SettledAt"),
                tag(document, "CreDtTm")
        );

        return response;
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

    private PaymentStatus mapStatus(String sorbnetStatus) {
        if (sorbnetStatus == null || sorbnetStatus.isBlank()) {
            log.warn("Empty SORBNet status received. Mapping to BLOCKED.");
            return PaymentStatus.BLOCKED;
        }

        return switch (sorbnetStatus.trim().toUpperCase()) {
            case "SETTLED", "ACSC", "ACCP", "ACSP" -> PaymentStatus.PROCESSED;
            case "REJECTED", "RJCT" -> PaymentStatus.REJECTED;
            case "GRIDLOCK_HELD", "BLOCKED", "BLCK", "PDNG" -> PaymentStatus.BLOCKED;
            default -> {
                log.warn("Unknown SORBNet status received: {}. Mapping to BLOCKED.", sorbnetStatus);
                yield PaymentStatus.BLOCKED;
            }
        };
    }

    private static class SorbnetResponse {
        private String paymentId;
        private String status;
        private String message;
        private String settledAt;
    }
}