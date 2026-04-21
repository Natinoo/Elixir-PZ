package pl.pz.sorbnet;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SorbnetKafkaConsumer {

    @KafkaListener(topics = "payments.sorbnet", groupId = "sorbnet-group")
    public void consume(String message) {
        System.out.println(">>> SORBNET received payment: " + message);
    }
}