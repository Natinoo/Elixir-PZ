package pl.pz.elixir.consumer;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.pz.elixir.dto.SorbnetPaymentResponseDto;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;

@Component
public class SorbnetResponseConsumer {
    

    private static final Logger log = LoggerFactory.getLogger(SorbnetResponseConsumer.class);

    private final XmlMapper xmlMapper;
    private final ElixirPaymentService elixirPaymentService;

    public SorbnetResponseConsumer(XmlMapper xmlMapper,
                                   ElixirPaymentService elixirPaymentService) {
        this.xmlMapper = xmlMapper;
        this.elixirPaymentService = elixirPaymentService;
    }

    @KafkaListener(topics = "responses.elixir", groupId = "elixir-group")
    public void consume(String message) {
        try {
            SorbnetPaymentResponseDto response =
                    xmlMapper.readValue(message, SorbnetPaymentResponseDto.class);

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