package pl.pz.sorbnet.messeging;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import pl.pz.sorbnet.dto.PaymentResponseDto;
import pl.pz.sorbnet.dto.SorbnetPaymentDto;
import pl.pz.sorbnet.service.SorbnetPaymentService;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class SorbnetKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SorbnetKafkaConsumer.class);
    

    private final Unmarshaller unmarshaller;
    private final SorbnetPaymentService paymentService;
    private final SimpMessagingTemplate ws;
    private final IntegrationResponseProducer responseProducer;
    private final XmlMapper xmlMapper = new XmlMapper();

    public SorbnetKafkaConsumer(SorbnetPaymentService paymentService,
                                SimpMessagingTemplate ws,
                                IntegrationResponseProducer responseProducer
                                ) throws JAXBException {
        this.paymentService = paymentService;
        this.ws = ws;
        this.responseProducer = responseProducer;
        this.unmarshaller = JAXBContext.newInstance(SorbnetPaymentDto.class).createUnmarshaller();
    }

    @KafkaListener(topics = "payments.sorbnet", groupId = "sorbnet-group")
    public void onPaymentFromElixir(ConsumerRecord<String, String> record) {
        process(record, "ELIXIR");
    }

    @KafkaListener(topics = "payments.express.sorbnet", groupId = "sorbnet-group")
    public void onPaymentFromExpress(ConsumerRecord<String, String> record) {
        process(record, "ELIXIR_EXPRESS");
    }

    private void process(ConsumerRecord<String, String> record, String source) {
        log.info("[PAYMENT][{}] key={} payload={}", source, record.key(), record.value());

        try {
            SorbnetPaymentDto dto = unmarshaller.unmarshal(
                    new StreamSource(new StringReader(record.value())),
                    SorbnetPaymentDto.class
            ).getValue();

            Map<String, Object> result = paymentService.process(dto);
            ws.convertAndSend("/topic/payments", result);

            String paymentId = String.valueOf(result.getOrDefault("paymentId", record.key()));
            String status = String.valueOf(result.getOrDefault("status", "UNKNOWN"));
            String message = String.valueOf(result.getOrDefault("message", "Payment processed"));

            PaymentResponseDto responseDto = new PaymentResponseDto();
            responseDto.setPaymentId(paymentId);
            responseDto.setStatus(status);
            responseDto.setMessage(message);
            responseDto.setSenderBankId(dto.getSenderBankId());
            responseDto.setReceiverBankId(dto.getReceiverBankId());
            responseDto.setAmount(dto.getAmount());
            responseDto.setSettledAt(LocalDateTime.now().toString());

            String responseXml = xmlMapper.writeValueAsString(responseDto);
            log.info("[PAYMENT][{}] response payload={}", source, responseXml);

            if ("ELIXIR_EXPRESS".equals(source)) {
                responseProducer.sendToExpress(paymentId, responseXml);
            } else {
                responseProducer.sendToElixir(paymentId, responseXml);
            }

            log.info("[PAYMENT][{}] response sent paymentId={} status={}", source, paymentId, status);

        } catch (JAXBException e) {
            log.error("[PAYMENT][{}] XML parse error payload={}", source, record.value(), e);
            sendError(source, record.key(), "XML parse error");
        } catch (Exception e) {
            log.error("[PAYMENT][{}] processing error payload={}", source, record.value(), e);
            sendError(source, record.key(), "Processing error: " + e.getMessage());
        }
    }

    private void sendError(String source, String paymentId, String message) {
        try {
            PaymentResponseDto responseDto = new PaymentResponseDto();
            responseDto.setPaymentId(paymentId);
            responseDto.setStatus("REJECTED");
            responseDto.setMessage(message);
            responseDto.setSettledAt(LocalDateTime.now().toString());

            String responseXml = xmlMapper.writeValueAsString(responseDto);
            log.info("[PAYMENT][{}] error response payload={}", source, responseXml);

            if ("ELIXIR_EXPRESS".equals(source)) {
                responseProducer.sendToExpress(paymentId, responseXml);
            } else {
                responseProducer.sendToElixir(paymentId, responseXml);
            }
        } catch (Exception e) {
            log.error("Cannot send XML error response for paymentId={}", paymentId, e);
        }
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