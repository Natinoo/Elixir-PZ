package pl.pz.sorbnet.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pl.pz.sorbnet.dto.SorbnetPaymentDto;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;

@Service
public class SorbnetPaymentService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public SorbnetPaymentService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Map<String, Object> process(SorbnetPaymentDto dto) {

        if (dto.getPaymentId() == null) {
            dto.setPaymentId(UUID.randomUUID().toString());
        }

        String xml = toXml(dto);

        // ✅ poprawny topic
        kafkaTemplate.send("payments.sorbnet", dto.getPaymentId(), xml);

        return Map.of(
                "paymentId", dto.getPaymentId(),
                "status", "SENT_TO_SORBNET"
        );
    }

    private String toXml(SorbnetPaymentDto dto) {
        try {
            JAXBContext context = JAXBContext.newInstance(SorbnetPaymentDto.class);
            Marshaller marshaller = context.createMarshaller();

            StringWriter writer = new StringWriter();
            marshaller.marshal(dto, writer);

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}