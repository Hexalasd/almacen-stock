package com.almacen.dao;

import com.almacen.model.Product;
import com.almacen.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
    private static final Logger logger = LogManager.getLogger(ProductDAO.class);

    public List<Product> findAll() {
        String sql = "SELECT p.id, p.code, p.name, p.category_id, c.name as category_name, " +
                "p.supplier, p.location, p.purchase_price, p.purchase_unit, p.sale_price, p.sale_unit, " +
                "p.current_stock, p.min_stock_alert, p.created_at, p.updated_at " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.id ORDER BY p.id";
        List<Product> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error executing findAll products", e);
        }

        return result;
    }

    public Product findById(int id) {
        String sql = "SELECT p.id, p.code, p.name, p.category_id, c.name as category_name, " +
                "p.supplier, p.location, p.purchase_price, p.purchase_unit, p.sale_price, p.sale_unit, " +
                "p.current_stock, p.min_stock_alert, p.created_at, p.updated_at " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE p.id = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing findById product id={}", id, e);
        }

        return null;
    }

    public Product findByCode(String code) {
        String sql = "SELECT p.id, p.code, p.name, p.category_id, c.name as category_name, " +
                "p.supplier, p.location, p.purchase_price, p.purchase_unit, p.sale_price, p.sale_unit, " +
                "p.current_stock, p.min_stock_alert, p.created_at, p.updated_at " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE p.code = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing findByCode code={}", code, e);
        }

        return null;
    }

    public List<Product> findByCategory(Long categoryId) {
        String sql = "SELECT p.id, p.code, p.name, p.category_id, c.name as category_name, " +
                "p.supplier, p.location, p.purchase_price, p.purchase_unit, p.sale_price, p.sale_unit, " +
                "p.current_stock, p.min_stock_alert, p.created_at, p.updated_at " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE p.category_id = ? ORDER BY p.id";

        List<Product> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing findByCategory categoryId={}", categoryId, e);
        }

        return result;
    }

    public List<Product> findLowStock() {
        String sql = "SELECT p.id, p.code, p.name, p.category_id, c.name as category_name, " +
                "p.supplier, p.location, p.purchase_price, p.purchase_unit, p.sale_price, p.sale_unit, " +
                "p.current_stock, p.min_stock_alert, p.created_at, p.updated_at " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE p.current_stock <= p.min_stock_alert ORDER BY p.current_stock ASC";

        List<Product> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error executing findLowStock", e);
        }

        return result;
    }

    public int save(Product product) {
        String sql = "INSERT INTO products (code, name, category_id, supplier, location, purchase_price, purchase_unit, " +
                "sale_price, sale_unit, current_stock, min_stock_alert, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP), COALESCE(?, CURRENT_TIMESTAMP))";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getCode());
            ps.setString(2, product.getName());
            setNullableLong(ps, 3, product.getCategoryId());
            ps.setString(4, product.getSupplier());
            ps.setString(5, product.getLocation());

            setNullableDouble(ps, 6, product.getPurchasePrice());
            setNullableString(ps, 7, product.getPurchaseUnit() != null ? product.getPurchaseUnit().name() : null);
            setNullableDouble(ps, 8, product.getSalePrice());
            setNullableString(ps, 9, product.getSaleUnit() != null ? product.getSaleUnit().name() : null);
            setNullableInt(ps, 10, product.getCurrentStock());
            setNullableInt(ps, 11, product.getMinStockAlert());

            LocalDateTime createdAt = product.getCreatedAt();
            if (createdAt != null) {
                ps.setTimestamp(12, Timestamp.valueOf(createdAt));
            } else {
                ps.setTimestamp(12, null);
            }

            LocalDateTime updatedAt = product.getUpdatedAt();
            if (updatedAt != null) {
                ps.setTimestamp(13, Timestamp.valueOf(updatedAt));
            } else {
                ps.setTimestamp(13, null);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing save product={}", product, e);
        }

        return 0;
    }

    public boolean update(Product product) {
        String sql = "UPDATE products SET code = ?, name = ?, category_id = ?, supplier = ?, location = ?, " +
                "purchase_price = ?, purchase_unit = ?, sale_price = ?, sale_unit = ?, current_stock = ?, " +
                "min_stock_alert = ?, updated_at = COALESCE(?, CURRENT_TIMESTAMP) WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getCode());
            ps.setString(2, product.getName());
            setNullableLong(ps, 3, product.getCategoryId());
            ps.setString(4, product.getSupplier());
            ps.setString(5, product.getLocation());

            setNullableDouble(ps, 6, product.getPurchasePrice());
            setNullableString(ps, 7, product.getPurchaseUnit() != null ? product.getPurchaseUnit().name() : null);
            setNullableDouble(ps, 8, product.getSalePrice());
            setNullableString(ps, 9, product.getSaleUnit() != null ? product.getSaleUnit().name() : null);
            setNullableInt(ps, 10, product.getCurrentStock());
            setNullableInt(ps, 11, product.getMinStockAlert());

            LocalDateTime updatedAt = product.getUpdatedAt();
            if (updatedAt != null) {
                ps.setTimestamp(12, Timestamp.valueOf(updatedAt));
            } else {
                ps.setTimestamp(12, null);
            }

            ps.setLong(13, product.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error executing update product={}", product, e);
            return false;
        }
    }

    public boolean updateStock(int productId, int newStock) {
        String sql = "UPDATE products SET current_stock = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error executing updateStock productId={} newStock={}", productId, newStock, e);
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error executing delete product id={}", id, e);
            return false;
        }
    }

    private static Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setCode(rs.getString("code"));
        p.setName(rs.getString("name"));
        
        // Categoria
        Long categoryId = toNullableLong(rs, "category_id");
        p.setCategoryId(categoryId);
        p.setCategoryName(rs.getString("category_name"));
        
        p.setSupplier(rs.getString("supplier"));
        p.setLocation(rs.getString("location"));

        p.setPurchasePrice(toNullableDouble(rs, "purchase_price"));
        p.setSalePrice(toNullableDouble(rs, "sale_price"));
        
        // Unidades
        String purchaseUnitStr = rs.getString("purchase_unit");
        if (purchaseUnitStr != null) {
            try {
                p.setPurchaseUnit(Product.Unit.valueOf(purchaseUnitStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Unidad de compra inválida: {}", purchaseUnitStr);
            }
        }
        
        String saleUnitStr = rs.getString("sale_unit");
        if (saleUnitStr != null) {
            try {
                p.setSaleUnit(Product.Unit.valueOf(saleUnitStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Unidad de venta inválida: {}", saleUnitStr);
            }
        }
        
        p.setCurrentStock(toNullableInt(rs, "current_stock"));
        p.setMinStockAlert(toNullableInt(rs, "min_stock_alert"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            p.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            p.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return p;
    }

    private static void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.REAL);
        } else {
            ps.setDouble(index, value);
        }
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private static Double toNullableDouble(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        return o == null ? null : ((Number) o).doubleValue();
    }

    private static Integer toNullableInt(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        return o == null ? null : ((Number) o).intValue();
    }

    private static Long toNullableLong(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        return o == null ? null : ((Number) o).longValue();
    }
}

