package com.example.inventory.api;

import com.example.contracts.api.ProductSnapshot;
import com.example.inventory.domain.ProductRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final ProductRepository repository;

    public InventoryController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/products")
    public List<ProductSnapshot> products() {
        return repository.findAll()
                .stream()
                .map(p -> new ProductSnapshot(
                        p.getProductId(),
                        p.getName(),
                        p.getAvailableQuantity(),
                        p.getUnitPrice()))
                .toList();
    }
}
