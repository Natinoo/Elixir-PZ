package pl.pz.elixirexpress;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic elixirExpressTopic() {
        return TopicBuilder.name("payments.elixir-express")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic expressToSorbnetTopic() {
        return TopicBuilder.name("payments.express.sorbnet")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic expressResponsesTopic() {
        return TopicBuilder.name("responses.elixir-express")
                .partitions(1)
                .replicas(1)
                .build();
    }
}