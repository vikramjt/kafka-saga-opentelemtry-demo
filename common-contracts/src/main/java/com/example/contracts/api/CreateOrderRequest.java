package com.example.contracts.api;

import com.example.contracts.shared.OrderLine;
import java.util.List;

public record CreateOrderRequest(String customerId, List<OrderLine> items) {
}
