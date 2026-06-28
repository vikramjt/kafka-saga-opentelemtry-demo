package com.example.contracts.events;

import com.example.contracts.shared.ReservedItem;
import java.util.List;

public record BillingChargeRequestedEvent(
        String sagaId,
        String customerId,
        double totalAmount,
        List<ReservedItem> reservedItems) {
}
