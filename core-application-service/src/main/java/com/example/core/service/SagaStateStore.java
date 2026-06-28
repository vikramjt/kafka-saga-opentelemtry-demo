package com.example.core.service;

import com.example.contracts.api.OrderStatusResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SagaStateStore {

    private final Map<String, OrderStatusResponse> states = new ConcurrentHashMap<>();

    public void setPending(String sagaId) {
        states.put(sagaId, new OrderStatusResponse(sagaId, "PENDING", "Order accepted by core service"));
    }

    public void setState(String sagaId, String status, String message) {
        states.put(sagaId, new OrderStatusResponse(sagaId, status, message));
    }

    public OrderStatusResponse get(String sagaId) {
        return states.getOrDefault(sagaId,
                new OrderStatusResponse(sagaId, "UNKNOWN", "Saga id not found in core state"));
    }
}
