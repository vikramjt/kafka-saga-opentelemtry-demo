package com.example.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String productId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private double unitPrice;

    public ProductEntity() {
    }

    public ProductEntity(String productId, String name, int availableQuantity, double unitPrice) {
        this.productId = productId;
        this.name = name;
        this.availableQuantity = availableQuantity;
        this.unitPrice = unitPrice;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }
}
