package com.example.contracts.events;

public record InventoryReservationRejectedEvent(String sagaId, String reason) {
}
