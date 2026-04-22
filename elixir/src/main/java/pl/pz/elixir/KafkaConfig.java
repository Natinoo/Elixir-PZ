package pl.pz.elixir;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic elixirTopic() {
        return TopicBuilder.name("payments.elixir")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // do przeniesienia do elixiru-express
    @Bean
    public NewTopic elixirExpressTopic() {
        return TopicBuilder.name("payments.elixir-express")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // do przeniesienia do sorbnetu
    @Bean
    public NewTopic sorbnetTopic() {
        return TopicBuilder.name("payments.sorbnet")
                .partitions(1)
                .replicas(1)
                .build();
    }
}