package com.example.core.config;

import java.util.List;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    List<NewTopic> sagaTopics() {
        return List.of(
                TopicBuilder.name("order.commands.requested").partitions(6).replicas(1).build(),
                TopicBuilder.name("inventory.commands.reserve").partitions(6).replicas(1).build(),
                TopicBuilder.name("inventory.commands.release").partitions(6).replicas(1).build(),
                TopicBuilder.name("inventory.events.reserved").partitions(6).replicas(1).build(),
                TopicBuilder.name("inventory.events.rejected").partitions(6).replicas(1).build(),
                TopicBuilder.name("billing.commands.charge").partitions(6).replicas(1).build(),
                TopicBuilder.name("billing.events.charged").partitions(6).replicas(1).build(),
                TopicBuilder.name("billing.events.rejected").partitions(6).replicas(1).build(),
                TopicBuilder.name("order.events.finalized").partitions(6).replicas(1).build()
        );
    }
}
