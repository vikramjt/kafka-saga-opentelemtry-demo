package com.example.contracts.events;

public record BillingChargedEvent(String sagaId, String billingReference, double amount) {
}
