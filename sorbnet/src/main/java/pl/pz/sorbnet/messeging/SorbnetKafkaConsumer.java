package pl.pz.sorbnet.messeging;  

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SorbnetKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SorbnetKafkaConsumer.class);
    private final ObjectMapper mapper;

    public SorbnetKafkaConsumer(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    @KafkaListener(topics = "payments.sorbnet", groupId = "sorbnet-group")
    public void onPayment(ConsumerRecord<String, String> record) {
        log.info("[PAYMENT] key={} payload={}", record.key(), record.value());
    }
    
    @KafkaListener(topics = "notifications.banks", groupId = "sorbnet-group")
    public void onSettlement(ConsumerRecord<String, String> record) {
        log.info("[SETTLEMENT] bank={} payload={}", record.key(), record.value());
    }

    @KafkaListener(topics = "events.emergency", groupId = "sorbnet-group")
    public void onEmergency(ConsumerRecord<String, String> record) {
        log.warn("[EMERGENCY] bank={} payload={}", record.key(), record.value());
    }

    @KafkaListener(topics = "events.gridlock", groupId = "sorbnet-group")
    public void onGridlock(ConsumerRecord<String, String> record) {
        log.warn("[GRIDLOCK] bank={} payload={}", record.key(), record.value());
    }
}