package com.example.order.api;

import com.example.order.domain.OrderSagaEntity;
import com.example.order.domain.OrderSagaRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    private final OrderSagaRepository repository;

    public OrderQueryController(OrderSagaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<OrderSagaEntity> all() {
        return repository.findAll();
    }

    @GetMapping("/{sagaId}")
    public OrderSagaEntity one(@PathVariable String sagaId) {
        return repository.findById(sagaId).orElseThrow();
    }
}
