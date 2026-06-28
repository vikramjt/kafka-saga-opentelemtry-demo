package com.example.contracts.events;

import com.example.contracts.shared.ReservedItem;
import java.util.List;

public record InventoryReservedEvent(String sagaId, List<ReservedItem> reservedItems, double totalAmount) {
}
