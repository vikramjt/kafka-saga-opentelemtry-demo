package com.example.contracts.events;

import com.example.contracts.shared.ReservedItem;
import java.util.List;

public record InventoryReleaseRequestedEvent(String sagaId, String reason, List<ReservedItem> reservedItems) {
}
