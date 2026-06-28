package com.example.core.service;

import com.example.contracts.api.CreateOrderRequest;
import com.example.contracts.events.OrderCompletedEvent;
import com.example.contracts.events.OrderRequestedEvent;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CoreOrderOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CoreOrderOrchestrator.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SagaStateStore sagaStateStore;

    public CoreOrderOrchestrator(KafkaTemplate<String, Object> kafkaTemplate, SagaStateStore sagaStateStore) {
        this.kafkaTemplate = kafkaTemplate;
        this.sagaStateStore = sagaStateStore;
    }

    public String submitOrder(CreateOrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        sagaStateStore.setPending(sagaId);
        log.info("Publishing OrderRequestedEvent to topic=order.commands.requested, sagaId={}", sagaId);
        kafkaTemplate.send(
                "order.commands.requested",
                sagaId,
                new OrderRequestedEvent(sagaId, request.customerId(), request.items(), Instant.now()));
        return sagaId;
    }

    @KafkaListener(topics = "order.events.finalized", groupId = "core-application-service")
    public void consumeOrderFinalizedEvent(OrderCompletedEvent event) {
        log.info("Consumed OrderCompletedEvent from topic=order.events.finalized, sagaId={}, status={}",
                event.sagaId(), event.status());
        sagaStateStore.setState(event.sagaId(), event.status(), event.message());
    }
}
