package com.example.core.api;

import com.example.contracts.api.CreateOrderRequest;
import com.example.contracts.api.OrderStatusResponse;
import com.example.contracts.api.ProductSnapshot;
import com.example.core.service.CoreOrderOrchestrator;
import com.example.core.service.SagaStateStore;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api")
public class CoreController {

    private final CoreOrderOrchestrator orchestrator;
    private final SagaStateStore stateStore;
    private final RestClient restClient;

    public CoreController(
            CoreOrderOrchestrator orchestrator,
            SagaStateStore stateStore,
            @Value("${services.inventory.base-url}") String inventoryBaseUrl) {
        this.orchestrator = orchestrator;
        this.stateStore = stateStore;
        this.restClient = RestClient.builder().baseUrl(inventoryBaseUrl).build();
    }

    @GetMapping("/products")
    public List<ProductSnapshot> products() {
        ProductSnapshot[] snapshots = restClient.get()
                .uri("/api/inventory/products")
                .retrieve()
                .body(ProductSnapshot[].class);
        return snapshots == null ? List.of() : List.of(snapshots);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderStatusResponse order(@RequestBody CreateOrderRequest request) {
        String sagaId = orchestrator.submitOrder(request);
        return stateStore.get(sagaId);
    }

    @GetMapping("/orders/{sagaId}")
    public OrderStatusResponse orderStatus(@PathVariable String sagaId) {
        return stateStore.get(sagaId);
    }
}
