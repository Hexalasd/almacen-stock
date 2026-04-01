package com.almacen.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gestor centralizado de eventos de stock usando el patrón Observer
 */
public class StockEventManager {
    private static final StockEventManager INSTANCE = new StockEventManager();
    
    // Usar CopyOnWriteArrayList para evitar ConcurrentModificationException
    private final List<StockUpdateListener> listeners = new CopyOnWriteArrayList<>();
    
    private StockEventManager() {}
    
    public static StockEventManager getInstance() {
        return INSTANCE;
    }
    
    public void addListener(StockUpdateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(StockUpdateListener listener) {
        listeners.remove(listener);
    }
    
    public void notifyStockUpdated(Long productId) {
        for (StockUpdateListener listener : listeners) {
            try {
                listener.onStockUpdated(productId);
            } catch (Exception e) {
                // Log error but don't stop notifying other listeners
                System.err.println("Error notifying listener: " + e.getMessage());
            }
        }
    }
    
    public void notifyProductAdded(Long productId) {
        for (StockUpdateListener listener : listeners) {
            try {
                listener.onProductAdded(productId);
            } catch (Exception e) {
                System.err.println("Error notifying listener: " + e.getMessage());
            }
        }
    }
    
    public void notifyProductDeleted(Long productId) {
        for (StockUpdateListener listener : listeners) {
            try {
                listener.onProductDeleted(productId);
            } catch (Exception e) {
                System.err.println("Error notifying listener: " + e.getMessage());
            }
        }
    }
    
    public void notifyAllProductsChanged() {
        // Cuando todos los productos pueden haber cambiado, notificar como actualización general
        notifyStockUpdated(null);
    }
}
