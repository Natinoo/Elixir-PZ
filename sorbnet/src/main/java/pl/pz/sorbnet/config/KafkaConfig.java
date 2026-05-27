package pl.pz.sorbnet.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic paymentsTopic() {
        return TopicBuilder.name("payments.sorbnet").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic expressPaymentsTopic() {
        return TopicBuilder.name("payments.express.sorbnet").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name("notifications.banks").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic gridlockTopic() {
        return TopicBuilder.name("events.gridlock").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic emergencyTopic() {
        return TopicBuilder.name("events.emergency").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic responsesElixirTopic() {
        return TopicBuilder.name("responses.elixir").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic responsesElixirExpressTopic() {
        return TopicBuilder.name("responses.elixir-express").partitions(1).replicas(1).build();
    }
    @Bean
    public NewTopic elixirResponsesTopic() {
        return TopicBuilder.name("responses.elixir")
                .partitions(1)
                .replicas(1)
                .build();
    }
}