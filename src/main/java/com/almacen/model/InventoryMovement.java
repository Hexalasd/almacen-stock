package com.almacen.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class InventoryMovement {
    public enum MovementType { ENTRY, EXIT, ADJUSTMENT }

    private Long id;
    private Long productId;
    private Integer quantity;
    private MovementType type;
    private String reason;
    private Long userId;
    private LocalDateTime timestamp;

    public InventoryMovement() {
    }

    public InventoryMovement(Long id,
                             Long productId,
                             Integer quantity,
                             MovementType type,
                             String reason,
                             Long userId,
                             LocalDateTime timestamp) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.type = type;
        this.reason = reason;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public MovementType getType() {
        return type;
    }

    public void setType(MovementType type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "InventoryMovement{" +
                "id=" + id +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", type=" + type +
                ", reason='" + reason + '\'' +
                ", userId=" + userId +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryMovement that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
