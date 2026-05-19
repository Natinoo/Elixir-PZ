package pl.pz.sorbnet.messeging;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import pl.pz.sorbnet.dto.SorbnetPaymentDto;
import pl.pz.sorbnet.service.SorbnetPaymentService;

import java.io.StringReader;
import java.util.Map;

@Component
public class SorbnetKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SorbnetKafkaConsumer.class);
    private final Unmarshaller unmarshaller;
    private final SorbnetPaymentService paymentService;
    private final SimpMessagingTemplate ws;

    public SorbnetKafkaConsumer(SorbnetPaymentService paymentService,
                                SimpMessagingTemplate ws) throws JAXBException {
        this.paymentService = paymentService;
        this.ws = ws;
        this.unmarshaller = JAXBContext.newInstance(SorbnetPaymentDto.class).createUnmarshaller();
    }

    @KafkaListener(topics = "payments.sorbnet", groupId = "sorbnet-group")
    public void onPayment(ConsumerRecord<String, String> record) {
        log.info("[PAYMENT] key={} payload={}", record.key(), record.value());
        try {
            SorbnetPaymentDto dto = (SorbnetPaymentDto) unmarshaller
                    .unmarshal(new StringReader(record.value()));

            Map<String, Object> result = paymentService.process(dto);

            // broadcast wyniku do GUI — Dashboard.jsx słucha na /topic/payments
            ws.convertAndSend("/topic/payments", result);
            log.info("[PAYMENT] broadcast status={} paymentId={}",
                    result.get("status"), result.get("paymentId"));

        } catch (JAXBException e) {
            log.error("[PAYMENT] XML parse error — sprawdź nazwę root elementu. payload={}", record.value(), e);
        } catch (Exception e) {
            log.error("[PAYMENT] processing error: {}", record.value(), e);
        }
    }
    @KafkaListener(topics = "payments.sorbnet", groupId = "sorbnet-group")
    public void onPayment(ConsumerRecord<String, String> record) {
        log.info("[PAYMENT] key={} payload={}", record.key(), record.value());
    }
    
    @KafkaListener(topics = "notifications.banks", groupId = "sorbnet-group")
    public void onSettlement(ConsumerRecord<String, String> record) {
        log.info("[SETTLEMENT] bank={} payload={}", record.key(), record.value());
        ws.convertAndSend("/topic/settlements", record.value());
    }

    @KafkaListener(topics = "events.emergency", groupId = "sorbnet-group")
    public void onEmergency(ConsumerRecord<String, String> record) {
        log.warn("[EMERGENCY] bank={} payload={}", record.key(), record.value());
        ws.convertAndSend("/topic/emergency", record.value());
    }

    @KafkaListener(topics = "events.gridlock", groupId = "sorbnet-group")
    public void onGridlock(ConsumerRecord<String, String> record) {
        log.warn("[GRIDLOCK] bank={} payload={}", record.key(), record.value());
        ws.convertAndSend("/topic/gridlock", record.value());
    }
}