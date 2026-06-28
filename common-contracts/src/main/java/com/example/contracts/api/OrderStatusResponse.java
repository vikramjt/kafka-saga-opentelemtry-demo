package com.example.contracts.api;

public record OrderStatusResponse(String sagaId, String status, String message) {
}
