package com.example.contracts.events;

import com.example.contracts.shared.OrderLine;
import java.time.Instant;
import java.util.List;

public record OrderRequestedEvent(String sagaId, String customerId, List<OrderLine> items, Instant requestedAt) {
}
