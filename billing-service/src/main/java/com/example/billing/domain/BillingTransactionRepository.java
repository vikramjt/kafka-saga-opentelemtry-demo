package com.example.billing.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingTransactionRepository extends JpaRepository<BillingTransactionEntity, Long> {

    List<BillingTransactionEntity> findBySagaId(String sagaId);
}
