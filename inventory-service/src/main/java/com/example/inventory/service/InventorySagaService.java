package com.example.inventory.service;

import com.example.contracts.events.InventoryReleaseRequestedEvent;
import com.example.contracts.events.InventoryReserveRequestedEvent;
import com.example.contracts.events.InventoryReservedEvent;
import com.example.contracts.events.InventoryReservationRejectedEvent;
import com.example.contracts.shared.OrderLine;
import com.example.contracts.shared.ReservedItem;
import com.example.inventory.domain.ProductEntity;
import com.example.inventory.domain.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventorySagaService {

    private static final Logger log = LoggerFactory.getLogger(InventorySagaService.class);

    private final ProductRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventorySagaService(ProductRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "inventory.commands.reserve", groupId = "inventory-service")
    @Transactional
    public void reserve(InventoryReserveRequestedEvent event) {
        log.info("Consumed InventoryReserveRequestedEvent from topic=inventory.commands.reserve, sagaId={}", event.sagaId());
        List<ReservedItem> reservedItems = new ArrayList<>();
        double total = 0.0;

        for (OrderLine line : event.items()) {
            ProductEntity product = repository.findById(line.productId()).orElse(null);
            if (product == null || product.getAvailableQuantity() < line.quantity()) {
                log.info("Publishing InventoryReservationRejectedEvent to topic=inventory.events.rejected, sagaId={}",
                        event.sagaId());
                kafkaTemplate.send(
                        "inventory.events.rejected",
                        event.sagaId(),
                        new InventoryReservationRejectedEvent(
                                event.sagaId(),
                                "Product unavailable or insufficient stock for productId: " + line.productId()));
                return;
            }
        }

        for (OrderLine line : event.items()) {
            ProductEntity product = repository.findById(line.productId()).orElseThrow();
            product.setAvailableQuantity(product.getAvailableQuantity() - line.quantity());
            repository.save(product);
            ReservedItem reservedItem =
                    new ReservedItem(product.getProductId(), product.getName(), line.quantity(), product.getUnitPrice());
            reservedItems.add(reservedItem);
            total += reservedItem.quantity() * reservedItem.unitPrice();
        }

        log.info("Publishing InventoryReservedEvent to topic=inventory.events.reserved, sagaId={}", event.sagaId());
        kafkaTemplate.send(
                "inventory.events.reserved",
                event.sagaId(),
                new InventoryReservedEvent(event.sagaId(), reservedItems, total));
    }

    @KafkaListener(topics = "inventory.commands.release", groupId = "inventory-service")
    @Transactional
    public void release(InventoryReleaseRequestedEvent event) {
        log.info("Consumed InventoryReleaseRequestedEvent from topic=inventory.commands.release, sagaId={}", event.sagaId());
        for (ReservedItem item : event.reservedItems()) {
            ProductEntity product = repository.findById(item.productId()).orElse(null);
            if (product != null) {
                product.setAvailableQuantity(product.getAvailableQuantity() + item.quantity());
                repository.save(product);
            }
        }
    }
}
