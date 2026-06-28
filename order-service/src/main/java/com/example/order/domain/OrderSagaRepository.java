package com.example.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSagaRepository extends JpaRepository<OrderSagaEntity, String> {
}
