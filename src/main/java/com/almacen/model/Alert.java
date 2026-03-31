package com.almacen.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Alert {
    public enum AlertType { LOW_STOCK, OUT_OF_STOCK }

    private Long id;
    private Long productId;
    private AlertType type;
    private String message;
    private boolean resolved;
    private LocalDateTime createdAt;

    public Alert() {
    }

    public Alert(Long id, Long productId, AlertType type, String message, boolean resolved, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.type = type;
        this.message = message;
        this.resolved = resolved;
        this.createdAt = createdAt;
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

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Alert{" +
                "id=" + id +
                ", productId=" + productId +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", resolved=" + resolved +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alert alert)) return false;
        return Objects.equals(id, alert.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
