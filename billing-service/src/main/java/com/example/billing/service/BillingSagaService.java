package com.example.billing.service;

import com.example.billing.domain.BillingTransactionEntity;
import com.example.billing.domain.BillingTransactionRepository;
import com.example.contracts.events.BillingChargeRequestedEvent;
import com.example.contracts.events.BillingChargedEvent;
import com.example.contracts.events.BillingRejectedEvent;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingSagaService {

    private final BillingTransactionRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BillingSagaService(BillingTransactionRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "billing.commands.charge", groupId = "billing-service")
    @Transactional
    public void charge(BillingChargeRequestedEvent event) {
        BillingTransactionEntity transaction = new BillingTransactionEntity();
        transaction.setSagaId(event.sagaId());
        transaction.setCustomerId(event.customerId());
        transaction.setAmount(event.totalAmount());

        if (event.totalAmount() <= 0 || event.customerId().toUpperCase().contains("FAIL_BILLING")) {
            transaction.setStatus("FAILED");
            transaction.setMessage("Billing declined by billing service validation");
            repository.save(transaction);
            kafkaTemplate.send(
                    "billing.events.rejected",
                    event.sagaId(),
                    new BillingRejectedEvent(event.sagaId(), transaction.getMessage(), event.reservedItems()));
            return;
        }

        transaction.setStatus("SUCCESS");
        String billingReference = "BILL-" + UUID.randomUUID();
        transaction.setMessage("Billing processed: " + billingReference);
        repository.save(transaction);

        kafkaTemplate.send(
                "billing.events.charged",
                event.sagaId(),
                new BillingChargedEvent(event.sagaId(), billingReference, event.totalAmount()));
    }
}
