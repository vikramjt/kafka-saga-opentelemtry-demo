package com.example.billing.api;

import com.example.billing.domain.BillingTransactionEntity;
import com.example.billing.domain.BillingTransactionRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingTransactionRepository repository;

    public BillingController(BillingTransactionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/transactions")
    public List<BillingTransactionEntity> transactions() {
        return repository.findAll();
    }

    @GetMapping("/transactions/{sagaId}")
    public List<BillingTransactionEntity> bySaga(@PathVariable String sagaId) {
        return repository.findBySagaId(sagaId);
    }
}
