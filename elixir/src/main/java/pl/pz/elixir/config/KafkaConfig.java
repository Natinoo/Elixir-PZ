package pl.pz.elixir.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic elixirPaymentsTopic() {
        return TopicBuilder.name("payments.elixir")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic sorbnetPaymentsTopic() {
        return TopicBuilder.name("payments.sorbnet")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic elixirResponsesTopic() {
        return TopicBuilder.name("responses.elixir")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic sorbnetLiquidityRequestsTopic() {
        return TopicBuilder.name("liquidity.requests.sorbnet")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic elixirLiquidityResponsesTopic() {
        return TopicBuilder.name("liquidity.responses.elixir")
                .partitions(1)
                .replicas(1)
                .build();
    }
}