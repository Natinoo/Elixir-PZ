package pl.pz.elixirexpress.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pl.pz.elixirexpress.dto.ExpressPaymentDto;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpressPaymentService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ExpressPaymentService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Map<String, Object> process(ExpressPaymentDto dto) {

        if (dto.getPaymentId() == null) {
            dto.setPaymentId(UUID.randomUUID().toString());
        }

        String xml = toXml(dto);

        kafkaTemplate.send("payments.elixir-express", dto.getPaymentId(), xml);

        return Map.of(
                "paymentId", dto.getPaymentId(),
                "status", "SENT_TO_EXPRESS"
        );
    }

    private String toXml(ExpressPaymentDto dto) {
        try {
            JAXBContext context = JAXBContext.newInstance(ExpressPaymentDto.class);
            Marshaller marshaller = context.createMarshaller();

            StringWriter writer = new StringWriter();
            marshaller.marshal(dto, writer);

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}