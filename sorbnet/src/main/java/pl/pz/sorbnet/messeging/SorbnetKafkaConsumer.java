package pl.pz.sorbnet.messeging;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import pl.pz.sorbnet.dto.LiquidityTransferRequestDto;
import pl.pz.sorbnet.dto.PaymentResponseDto;
import pl.pz.sorbnet.dto.SorbnetPaymentDto;
import pl.pz.sorbnet.service.LiquidityService;
import pl.pz.sorbnet.service.SorbnetPaymentService;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class SorbnetKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SorbnetKafkaConsumer.class);

    private final JAXBContext jaxbContext;
    private final SorbnetPaymentService paymentService;
    private final LiquidityService liquidityService;
    private final SimpMessagingTemplate ws;
    private final IntegrationResponseProducer responseProducer;

    public SorbnetKafkaConsumer(SorbnetPaymentService paymentService,
                                LiquidityService liquidityService,
                                SimpMessagingTemplate ws,
                                IntegrationResponseProducer responseProducer) throws JAXBException {
        this.paymentService = paymentService;
        this.liquidityService = liquidityService;
        this.ws = ws;
        this.responseProducer = responseProducer;
        this.jaxbContext = JAXBContext.newInstance(
                SorbnetPaymentDto.class,
                LiquidityTransferRequestDto.class,
                PaymentResponseDto.class
        );
    }

    // ===================================================================
    // Przelewy ISO 20022 (pacs.008-style, root <Document>)
    // W nowym przepływie pojawiają się tu wyłącznie przelewy wymagające
    // rozrachunku w SORBNET (np. netting przy braku płynności) — normalne
    // sesje ELIXIR rozlicza lokalnie i nic nie wysyła.
    // ===================================================================

    @KafkaListener(topics = "payments.sorbnet", groupId = "sorbnet-group")
    public void onPaymentFromElixir(ConsumerRecord<String, String> record) {
        processPayment(record, "ELIXIR");
    }

    @KafkaListener(topics = "payments.express.sorbnet", groupId = "sorbnet-group")
    public void onPaymentFromExpress(ConsumerRecord<String, String> record) {
        processPayment(record, "ELIXIR_EXPRESS");
    }

    private void processPayment(ConsumerRecord<String, String> record, String source) {
        log.info("[PAYMENT][{}] key={} payload={}", source, record.key(), record.value());

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            SorbnetPaymentDto dto = unmarshaller.unmarshal(
                    new StreamSource(new StringReader(record.value())),
                    SorbnetPaymentDto.class
            ).getValue();

            // identyfikacja serwisu źródłowego: jeśli komunikat nie niesie
            // ServiceCode, ustalamy go po topicu
            if (dto.getType() == null || dto.getType().isBlank()) {
                dto.setType(source);
            }

            Map<String, Object> result = paymentService.process(dto);
            ws.convertAndSend("/topic/payments", result);

            String paymentId = String.valueOf(result.getOrDefault("paymentId", record.key()));
            String status = String.valueOf(result.getOrDefault("status", "UNKNOWN"));
            String message = String.valueOf(result.getOrDefault("message", "Payment processed"));

            PaymentResponseDto responseDto = new PaymentResponseDto();
            responseDto.setPaymentId(paymentId);
            responseDto.setStatus(status);
            responseDto.setMessage(message);
            responseDto.setSourceServiceCode(dto.getType());
            responseDto.setSenderBankId(
                    String.valueOf(result.getOrDefault("senderBankId", dto.getSenderBankId()))
            );
            responseDto.setReceiverBankId(
                    String.valueOf(result.getOrDefault("receiverBankId", dto.getReceiverBankId()))
            );
            responseDto.setSenderAccount(
                    String.valueOf(result.getOrDefault("senderAccount", dto.getSenderAccount()))
            );
            responseDto.setReceiverAccount(
                    String.valueOf(result.getOrDefault("receiverAccount", dto.getReceiverAccount()))
            );
            responseDto.setAmount((BigDecimal) result.getOrDefault("amount", dto.getAmount()));
            responseDto.setCurrency(dto.getCurrency());
            responseDto.setSettledAt(
                    String.valueOf(result.getOrDefault("settledAt", LocalDateTime.now().toString()))
            );

            String responseXml = toXml(responseDto);
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

    // ===================================================================
    // Requesty płynnościowe (camt.050-style) — nowy przepływ:
    // ELIXIR wysyła je TYLKO przy faktycznym braku środków w sesji.
    // Request ląduje w GUI SORBNET, operator banku wyklikuje przelew.
    // ===================================================================

    @KafkaListener(topics = "liquidity.requests.sorbnet", groupId = "sorbnet-group")
    public void onLiquidityFromElixir(ConsumerRecord<String, String> record) {
        processLiquidity(record, "ELIXIR");
    }

    // TODO: zweryfikuj nazwę topicu w kodzie ELIXIR EXPRESS — analogia do payments.express.sorbnet
    @KafkaListener(topics = "liquidity.requests.express.sorbnet", groupId = "sorbnet-group")
    public void onLiquidityFromExpress(ConsumerRecord<String, String> record) {
        processLiquidity(record, "ELIXIR_EXPRESS");
    }

    private void processLiquidity(ConsumerRecord<String, String> record, String source) {
        log.info("[LIQUIDITY][{}] key={} payload={}", source, record.key(), record.value());

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            LiquidityTransferRequestDto dto = unmarshaller.unmarshal(
                    new StreamSource(new StringReader(record.value())),
                    LiquidityTransferRequestDto.class
            ).getValue();

            liquidityService.registerRequest(dto, source);

        } catch (JAXBException e) {
            log.error("[LIQUIDITY][{}] XML parse error payload={}", source, record.value(), e);
        } catch (Exception e) {
            log.error("[LIQUIDITY][{}] processing error payload={}", source, record.value(), e);
        }
    }

    // ===================================================================
    // Pozostałe powiadomienia (bez zmian)
    // ===================================================================

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

    // ===================================================================
    // Pomocnicze
    // ===================================================================

    private String toXml(PaymentResponseDto responseDto) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            // PaymentResponseDto ma @XmlRootElement(name = "Document"),
            // więc marshalling bez opakowania w JAXBElement
            marshaller.marshal(responseDto, sw);
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("XML marshal error", e);
        }
    }

    private void sendError(String source, String paymentId, String message) {
        try {
            PaymentResponseDto responseDto = new PaymentResponseDto();
            responseDto.setPaymentId(paymentId);
            responseDto.setStatus("REJECTED");
            responseDto.setMessage(message);
            responseDto.setSourceServiceCode(source);
            responseDto.setSettledAt(LocalDateTime.now().toString());

            String responseXml = toXml(responseDto);
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
}