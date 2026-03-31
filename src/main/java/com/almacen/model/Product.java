package com.almacen.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Product {
    private Long id;
    private String code;
    private String name;
    private String category;
    private String supplier;
    private String location;
    private Double purchasePrice;
    private Double salePrice;
    private Integer currentStock;
    private Integer minStockAlert;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {
    }

    public Product(Long id,
                   String code,
                   String name,
                   String category,
                   String supplier,
                   String location,
                   Double purchasePrice,
                   Double salePrice,
                   Integer currentStock,
                   Integer minStockAlert,
                   LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.category = category;
        this.supplier = supplier;
        this.location = location;
        this.purchasePrice = purchasePrice;
        this.salePrice = salePrice;
        this.currentStock = currentStock;
        this.minStockAlert = minStockAlert;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isLowStock() {
        int stock = currentStock == null ? 0 : currentStock;
        int min = minStockAlert == null ? 0 : minStockAlert;
        return stock <= min;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Double salePrice) {
        this.salePrice = salePrice;
    }

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public Integer getMinStockAlert() {
        return minStockAlert;
    }

    public void setMinStockAlert(Integer minStockAlert) {
        this.minStockAlert = minStockAlert;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", supplier='" + supplier + '\'' +
                ", location='" + location + '\'' +
                ", purchasePrice=" + purchasePrice +
                ", salePrice=" + salePrice +
                ", currentStock=" + currentStock +
                ", minStockAlert=" + minStockAlert +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return Objects.equals(id, product.id) && Objects.equals(code, product.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
}
