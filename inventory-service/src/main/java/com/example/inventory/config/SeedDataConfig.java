package com.example.inventory.config;

import com.example.inventory.domain.ProductEntity;
import com.example.inventory.domain.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedDataConfig {

    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new ProductEntity("P-100", "Laptop", 30, 999.00));
                repository.save(new ProductEntity("P-200", "Keyboard", 120, 49.00));
                repository.save(new ProductEntity("P-300", "Headset", 75, 69.00));
                repository.save(new ProductEntity("P-400", "Monitor", 45, 219.00));
            }
        };
    }
}
