package com.example.contracts.events;

import com.example.contracts.shared.ReservedItem;
import java.util.List;

public record BillingRejectedEvent(String sagaId, String reason, List<ReservedItem> reservedItems) {
}
