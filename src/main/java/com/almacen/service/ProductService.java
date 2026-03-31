package com.almacen.service;

import com.almacen.dao.ProductDAO;
import com.almacen.model.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductService {
    private static final Logger logger = LogManager.getLogger(ProductService.class);

    private final ProductDAO productDAO;

    public ProductService() {
        this.productDAO = new ProductDAO();
    }

    public Product createProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product cannot be null");
        }
        if (product.getCode() == null || product.getCode().isBlank()) {
            throw new IllegalArgumentException("product code is required");
        }
        if (productDAO.findByCode(product.getCode()) != null) {
            throw new IllegalArgumentException("Product code already exists: " + product.getCode());
        }

        int id = productDAO.save(product);
        if (id <= 0) {
            logger.error("Failed to create product with code={}", product.getCode());
            return null;
        }

        product.setId((long) id);
        logger.info("Product created id={} code={}", id, product.getCode());
        return product;
    }

    public boolean updateProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("product and product.id are required");
        }

        boolean ok = productDAO.update(product);
        if (ok) {
            logger.info("Product updated id={}", product.getId());
        } else {
            logger.warn("Product update failed id={}", product.getId());
        }
        return ok;
    }

    public boolean deleteProduct(int id) {
        boolean ok = productDAO.delete(id);
        if (ok) {
            logger.info("Product deleted id={}", id);
        } else {
            logger.warn("Product delete failed id={}", id);
        }
        return ok;
    }

    public Product getProductById(int id) {
        return productDAO.findById(id);
    }

    public Product getProductByCode(String code) {
        return productDAO.findByCode(code);
    }

    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllProducts();
        }

        String k = keyword.toLowerCase(Locale.ROOT).trim();
        List<Product> all = productDAO.findAll();
        List<Product> result = new ArrayList<>();

        for (Product p : all) {
            String name = p.getName() == null ? "" : p.getName();
            String code = p.getCode() == null ? "" : p.getCode();
            if (name.toLowerCase(Locale.ROOT).contains(k) || code.toLowerCase(Locale.ROOT).contains(k)) {
                result.add(p);
            }
        }

        return result;
    }

    public List<Product> getLowStockProducts() {
        return productDAO.findLowStock();
    }
}

