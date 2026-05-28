package pl.pz.elixir.consumer;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.pz.elixir.dto.SorbnetPaymentResponseDto;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;

import java.io.StringReader;

@Component
public class SorbnetResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(SorbnetResponseConsumer.class);

    private final ElixirPaymentService elixirPaymentService;
    private final JAXBContext jaxbContext;

    public SorbnetResponseConsumer(ElixirPaymentService elixirPaymentService) throws Exception {
        this.elixirPaymentService = elixirPaymentService;
        this.jaxbContext = JAXBContext.newInstance(SorbnetPaymentResponseDto.class);
    }

    @KafkaListener(topics = "responses.elixir", groupId = "elixir-group")
    public void consume(String message) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            SorbnetPaymentResponseDto response = (SorbnetPaymentResponseDto) unmarshaller.unmarshal(new StringReader(message));

            PaymentStatus mappedStatus = mapStatus(response.getStatus());

            elixirPaymentService.updatePaymentStatus(
                    response.getPaymentId(),
                    mappedStatus
            );

            log.info(
                    "SORBNet response processed: paymentId={}, sorbnetStatus={}, mappedStatus={}, settledAt={}",
                    response.getPaymentId(),
                    response.getStatus(),
                    mappedStatus,
                    response.getSettledAt()
            );

        } catch (Exception e) {
            log.error("Cannot process SORBNet XML response: {}", message, e);
        }
    }

    private PaymentStatus mapStatus(String sorbnetStatus) {
        return switch (sorbnetStatus) {
            case "SETTLED" -> PaymentStatus.PROCESSED;
            case "REJECTED" -> PaymentStatus.REJECTED;
            case "GRIDLOCK_HELD" -> PaymentStatus.BLOCKED;
            default -> throw new IllegalArgumentException("Unknown SORBNet status: " + sorbnetStatus);
        };
    }
}