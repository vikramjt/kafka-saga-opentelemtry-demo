package com.example.contracts.events;

import com.example.contracts.shared.OrderLine;
import java.util.List;

public record InventoryReserveRequestedEvent(String sagaId, List<OrderLine> items) {
}
