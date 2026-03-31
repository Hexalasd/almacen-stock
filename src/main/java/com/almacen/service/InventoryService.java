package com.almacen.service;

import com.almacen.dao.AlertDAO;
import com.almacen.dao.InventoryMovementDAO;
import com.almacen.dao.ProductDAO;
import com.almacen.model.InventoryMovement;
import com.almacen.model.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.List;

public class InventoryService {
    private static final Logger logger = LogManager.getLogger(InventoryService.class);

    private final ProductDAO productDAO;
    private final InventoryMovementDAO movementDAO;
    private final AlertDAO alertDAO;

    public InventoryService() {
        this.productDAO = new ProductDAO();
        this.movementDAO = new InventoryMovementDAO();
        this.alertDAO = new AlertDAO();
    }

    public boolean addStock(int productId, int quantity, String reason, int userId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }

        Product product = productDAO.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("product not found: " + productId);
        }

        int current = getCurrentStock(productId);
        int newStock = current + quantity;

        boolean updated = productDAO.updateStock(productId, newStock);
        if (!updated) {
            logger.error("Failed to update stock (add) productId={} current={} quantity={}", productId, current, quantity);
            return false;
        }

        InventoryMovement mv = new InventoryMovement();
        mv.setProductId((long) productId);
        mv.setQuantity(quantity);
        mv.setType(InventoryMovement.MovementType.ENTRY);
        mv.setReason(reason);
        mv.setUserId((long) userId);
        mv.setTimestamp(LocalDateTime.now());

        int moveId = movementDAO.save(mv);
        if (moveId <= 0) {
            logger.error("Failed to save inventory movement (ENTRY) productId={} quantity={}", productId, quantity);
            return false;
        }

        logger.info("Stock added productId={} quantity={} newStock={}", productId, quantity, newStock);
        return true;
    }

    public boolean removeStock(int productId, int quantity, String reason, int userId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }

        Product product = productDAO.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("product not found: " + productId);
        }

        int current = getCurrentStock(productId);
        if (current < quantity) {
            throw new IllegalStateException("Insufficient stock. Current=" + current + " requested=" + quantity);
        }

        int newStock = current - quantity;
        boolean updated = productDAO.updateStock(productId, newStock);
        if (!updated) {
            logger.error("Failed to update stock (remove) productId={} current={} quantity={}", productId, current, quantity);
            return false;
        }

        InventoryMovement mv = new InventoryMovement();
        mv.setProductId((long) productId);
        mv.setQuantity(quantity);
        mv.setType(InventoryMovement.MovementType.EXIT);
        mv.setReason(reason);
        mv.setUserId((long) userId);
        mv.setTimestamp(LocalDateTime.now());

        int moveId = movementDAO.save(mv);
        if (moveId <= 0) {
            logger.error("Failed to save inventory movement (EXIT) productId={} quantity={}", productId, quantity);
            return false;
        }

        logger.info("Stock removed productId={} quantity={} newStock={}", productId, quantity, newStock);
        return true;
    }

    public boolean adjustStock(int productId, int newQuantity, String reason, int userId) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("newQuantity must be >= 0");
        }

        Product product = productDAO.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("product not found: " + productId);
        }

        int current = getCurrentStock(productId);
        int delta = newQuantity - current;

        boolean updated = productDAO.updateStock(productId, newQuantity);
        if (!updated) {
            logger.error("Failed to update stock (adjust) productId={} current={} newQuantity={}", productId, current, newQuantity);
            return false;
        }

        InventoryMovement mv = new InventoryMovement();
        mv.setProductId((long) productId);
        mv.setQuantity(delta);
        mv.setType(InventoryMovement.MovementType.ADJUSTMENT);
        mv.setReason(reason);
        mv.setUserId((long) userId);
        mv.setTimestamp(LocalDateTime.now());

        int moveId = movementDAO.save(mv);
        if (moveId <= 0) {
            logger.error("Failed to save inventory movement (ADJUSTMENT) productId={} delta={}", productId, delta);
            return false;
        }

        logger.warn("Stock adjusted productId={} current={} newQuantity={} delta={}", productId, current, newQuantity, delta);
        return true;
    }

    public List<InventoryMovement> getMovementHistory(int productId) {
        return movementDAO.findByProductId(productId);
    }

    public int getCurrentStock(int productId) {
        int computed = movementDAO.getCurrentStock(productId);
        Integer productStock = null;
        Product p = productDAO.findById(productId);
        if (p != null) {
            productStock = p.getCurrentStock();
        }

        if (productStock != null && productStock != computed) {
            logger.warn("Stock mismatch productId={} products.current_stock={} computedFromMovements={}",
                    productId, productStock, computed);
        }

        return productStock != null ? productStock : computed;
    }
}

