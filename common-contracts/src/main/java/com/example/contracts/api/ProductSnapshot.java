package com.example.contracts.api;

public record ProductSnapshot(String productId, String name, int availableQuantity, double unitPrice) {
}
