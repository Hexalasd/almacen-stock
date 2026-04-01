package com.almacen.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Product {
    public enum Unit {
        KG("kg"),
        UNIDAD("unidad"),
        LITRO("litro"),
        GRAMO("gramo"),
        DOCENA("docena");
        
        private final String displayName;
        
        Unit(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    private Long id;
    private String code;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String supplier;
    private String location;
    private Double purchasePrice;
    private Unit purchaseUnit;
    private Double salePrice;
    private Unit saleUnit;
    private Integer currentStock;
    private Integer minStockAlert;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {
    }

    public Product(Long id,
                   String code,
                   String name,
                   Long categoryId,
                   String categoryName,
                   String supplier,
                   String location,
                   Double purchasePrice,
                   Unit purchaseUnit,
                   Double salePrice,
                   Unit saleUnit,
                   Integer currentStock,
                   Integer minStockAlert,
                   LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.supplier = supplier;
        this.location = location;
        this.purchasePrice = purchasePrice;
        this.purchaseUnit = purchaseUnit;
        this.salePrice = salePrice;
        this.saleUnit = saleUnit;
        this.currentStock = currentStock;
        this.minStockAlert = minStockAlert;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getCategoryDisplay() {
        return categoryName != null ? categoryName : "Sin categoría";
    }
    
    public String getPurchasePriceWithUnit() {
        if (purchasePrice == null) return "-";
        String unitStr = purchaseUnit != null ? "/" + purchaseUnit.getDisplayName() : "";
        return String.format("$%.2f%s", purchasePrice, unitStr);
    }
    
    public String getSalePriceWithUnit() {
        if (salePrice == null) return "-";
        String unitStr = saleUnit != null ? "/" + saleUnit.getDisplayName() : "";
        return String.format("$%.2f%s", salePrice, unitStr);
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Unit getPurchaseUnit() {
        return purchaseUnit;
    }

    public void setPurchaseUnit(Unit purchaseUnit) {
        this.purchaseUnit = purchaseUnit;
    }

    public Unit getSaleUnit() {
        return saleUnit;
    }

    public void setSaleUnit(Unit saleUnit) {
        this.saleUnit = saleUnit;
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
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", supplier='" + supplier + '\'' +
                ", location='" + location + '\'' +
                ", purchasePrice=" + purchasePrice +
                ", purchaseUnit=" + purchaseUnit +
                ", salePrice=" + salePrice +
                ", saleUnit=" + saleUnit +
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
