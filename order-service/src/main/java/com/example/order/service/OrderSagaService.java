package com.example.order.service;

import com.example.contracts.events.BillingChargeRequestedEvent;
import com.example.contracts.events.BillingChargedEvent;
import com.example.contracts.events.BillingRejectedEvent;
import com.example.contracts.events.InventoryReleaseRequestedEvent;
import com.example.contracts.events.InventoryReserveRequestedEvent;
import com.example.contracts.events.InventoryReservedEvent;
import com.example.contracts.events.InventoryReservationRejectedEvent;
import com.example.contracts.events.OrderCompletedEvent;
import com.example.contracts.events.OrderRequestedEvent;
import com.example.order.domain.OrderSagaEntity;
import com.example.order.domain.OrderSagaRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderSagaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderSagaRepository repository;

    public OrderSagaService(KafkaTemplate<String, Object> kafkaTemplate, OrderSagaRepository repository) {
        this.kafkaTemplate = kafkaTemplate;
        this.repository = repository;
    }

    @KafkaListener(topics = "order.commands.requested", groupId = "order-service")
    @Transactional
    public void onOrderRequested(OrderRequestedEvent event) {
        OrderSagaEntity saga = new OrderSagaEntity();
        saga.setSagaId(event.sagaId());
        saga.setCustomerId(event.customerId());
        saga.setStatus("ORDER_RECEIVED");
        saga.setMessage("Order captured and sent to inventory reservation");
        saga.setTotalAmount(0.0);
        repository.save(saga);

        kafkaTemplate.send(
                "inventory.commands.reserve",
                event.sagaId(),
                new InventoryReserveRequestedEvent(event.sagaId(), event.items()));
    }

    @KafkaListener(topics = "inventory.events.reserved", groupId = "order-service")
    @Transactional
    public void onInventoryReserved(InventoryReservedEvent event) {
        OrderSagaEntity saga = repository.findById(event.sagaId()).orElseThrow();
        saga.setStatus("INVENTORY_RESERVED");
        saga.setMessage("Inventory reserved. Billing request emitted");
        saga.setTotalAmount(event.totalAmount());
        repository.save(saga);

        kafkaTemplate.send(
                "billing.commands.charge",
                event.sagaId(),
                new BillingChargeRequestedEvent(
                        event.sagaId(),
                        saga.getCustomerId(),
                        event.totalAmount(),
                        event.reservedItems()));
    }

    @KafkaListener(topics = "inventory.events.rejected", groupId = "order-service")
    @Transactional
    public void onInventoryRejected(InventoryReservationRejectedEvent event) {
        OrderSagaEntity saga = repository.findById(event.sagaId()).orElseThrow();
        saga.setStatus("FAILED");
        saga.setMessage("Inventory reservation failed: " + event.reason());
        repository.save(saga);

        kafkaTemplate.send(
                "order.events.finalized",
                event.sagaId(),
                new OrderCompletedEvent(event.sagaId(), "FAILED", saga.getMessage()));
    }

    @KafkaListener(topics = "billing.events.charged", groupId = "order-service")
    @Transactional
    public void onBillingCharged(BillingChargedEvent event) {
        OrderSagaEntity saga = repository.findById(event.sagaId()).orElseThrow();
        saga.setStatus("COMPLETED");
        saga.setMessage("Order completed. Billing reference: " + event.billingReference());
        saga.setTotalAmount(event.amount());
        repository.save(saga);

        kafkaTemplate.send(
                "order.events.finalized",
                event.sagaId(),
                new OrderCompletedEvent(event.sagaId(), "COMPLETED", saga.getMessage()));
    }

    @KafkaListener(topics = "billing.events.rejected", groupId = "order-service")
    @Transactional
    public void onBillingRejected(BillingRejectedEvent event) {
        OrderSagaEntity saga = repository.findById(event.sagaId()).orElseThrow();
        saga.setStatus("FAILED");
        saga.setMessage("Billing failed: " + event.reason() + ". Triggering inventory compensation");
        repository.save(saga);

        kafkaTemplate.send(
                "inventory.commands.release",
                event.sagaId(),
                new InventoryReleaseRequestedEvent(event.sagaId(), "Billing rejected", event.reservedItems()));

        kafkaTemplate.send(
                "order.events.finalized",
                event.sagaId(),
                new OrderCompletedEvent(event.sagaId(), "FAILED", saga.getMessage()));
    }
}
