package pl.pz.sorbnet;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    
    @Bean
    public NewTopic sorbnetTopic() {
        return TopicBuilder.name("payments.sorbnet")
                .partitions(1)
                .replicas(1)
                .build();
    }
}