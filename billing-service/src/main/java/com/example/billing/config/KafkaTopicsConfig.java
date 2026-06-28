package com.example.billing.config;

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
                TopicBuilder.name("billing.commands.charge").partitions(6).replicas(1).build(),
                TopicBuilder.name("billing.events.charged").partitions(6).replicas(1).build(),
                TopicBuilder.name("billing.events.rejected").partitions(6).replicas(1).build()
        );
    }
}
