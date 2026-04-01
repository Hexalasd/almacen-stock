package com.almacen.events;

/**
 * Interfaz para notificar actualizaciones de stock entre paneles
 */
public interface StockUpdateListener {
    /**
     * Se llama cuando el stock de un producto ha sido actualizado
     * @param productId ID del producto actualizado
     */
    void onStockUpdated(Long productId);
    
    /**
     * Se llama cuando se ha agregado un nuevo producto
     * @param productId ID del nuevo producto
     */
    void onProductAdded(Long productId);
    
    /**
     * Se llama cuando un producto ha sido eliminado
     * @param productId ID del producto eliminado
     */
    void onProductDeleted(Long productId);
}
