package com.almacen.service;

import com.almacen.dao.AlertDAO;
import com.almacen.dao.ProductDAO;
import com.almacen.model.Alert;
import com.almacen.model.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AlertService {
    private static final Logger logger = LogManager.getLogger(AlertService.class);

    private final ProductDAO productDAO;
    private final AlertDAO alertDAO;
    private final Timer timer;

    public AlertService() {
        this.productDAO = new ProductDAO();
        this.alertDAO = new AlertDAO();
        this.timer = new Timer("alert-check-timer", true);
    }

    public List<Alert> checkAndGenerateAlerts() {
        List<Product> lowStock = productDAO.findLowStock();
        List<Alert> created = new ArrayList<>();

        for (Product p : lowStock) {
            if (p.getId() == null) continue;

            int stock = p.getCurrentStock() == null ? 0 : p.getCurrentStock();
            Alert.AlertType type = stock <= 0 ? Alert.AlertType.OUT_OF_STOCK : Alert.AlertType.LOW_STOCK;

            if (existsActiveAlertForProduct(p.getId().intValue(), type)) {
                continue;
            }

            Alert a = new Alert();
            a.setProductId(p.getId());
            a.setType(type);
            a.setResolved(false);
            a.setCreatedAt(LocalDateTime.now());

            if (type == Alert.AlertType.OUT_OF_STOCK) {
                a.setMessage("Producto sin stock: " + safe(p.getCode()) + " - " + safe(p.getName()));
            } else {
                a.setMessage("Stock bajo: " + safe(p.getCode()) + " - " + safe(p.getName()) +
                        " (stock=" + stock + ", min=" + (p.getMinStockAlert() == null ? 0 : p.getMinStockAlert()) + ")");
            }

            int id = alertDAO.save(a);
            if (id > 0) {
                a.setId((long) id);
                created.add(a);
                logger.warn("Alert created id={} productId={} type={}", id, p.getId(), type);
            } else {
                logger.error("Failed to create alert for productId={} type={}", p.getId(), type);
            }
        }

        return created;
    }

    public List<Alert> getActiveAlerts() {
        return alertDAO.findAllUnresolved();
    }

    public boolean markAlertResolved(int alertId) {
        boolean ok = alertDAO.resolve(alertId);
        if (ok) {
            logger.info("Alert resolved alertId={}", alertId);
        } else {
            logger.warn("Alert resolve failed alertId={}", alertId);
        }
        return ok;
    }

    public TimerTask scheduleAlertCheck() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    List<Alert> created = checkAndGenerateAlerts();
                    if (!created.isEmpty()) {
                        logger.info("Scheduled alert check created {} alerts", created.size());
                    }
                } catch (Exception e) {
                    logger.error("Scheduled alert check failed", e);
                }
            }
        };

        long delayMs = 0L;
        long periodMs = 30L * 60L * 1000L;
        timer.scheduleAtFixedRate(task, delayMs, periodMs);
        logger.info("Alert check scheduled every 30 minutes");
        return task;
    }

    private boolean existsActiveAlertForProduct(int productId, Alert.AlertType type) {
        List<Alert> alerts = alertDAO.findByProductId(productId);
        for (Alert a : alerts) {
            if (!a.isResolved() && a.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

