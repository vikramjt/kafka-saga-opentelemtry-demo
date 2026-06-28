package com.example.contracts.events;

public record OrderCompletedEvent(String sagaId, String status, String message) {
}
