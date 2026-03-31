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
        String sql = "SELECT id, code, name, category, supplier, location, purchase_price, sale_price, " +
                "current_stock, min_stock_alert, created_at, updated_at FROM products ORDER BY id";
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
        String sql = "SELECT id, code, name, category, supplier, location, purchase_price, sale_price, " +
                "current_stock, min_stock_alert, created_at, updated_at FROM products WHERE id = ? LIMIT 1";

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
        String sql = "SELECT id, code, name, category, supplier, location, purchase_price, sale_price, " +
                "current_stock, min_stock_alert, created_at, updated_at FROM products WHERE code = ? LIMIT 1";

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

    public List<Product> findByCategory(String category) {
        String sql = "SELECT id, code, name, category, supplier, location, purchase_price, sale_price, " +
                "current_stock, min_stock_alert, created_at, updated_at FROM products WHERE category = ? ORDER BY id";

        List<Product> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing findByCategory category={}", category, e);
        }

        return result;
    }

    public List<Product> findLowStock() {
        String sql = "SELECT id, code, name, category, supplier, location, purchase_price, sale_price, " +
                "current_stock, min_stock_alert, created_at, updated_at FROM products " +
                "WHERE current_stock <= min_stock_alert ORDER BY current_stock ASC";

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
        String sql = "INSERT INTO products (code, name, category, supplier, location, purchase_price, sale_price, " +
                "current_stock, min_stock_alert, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "COALESCE(?, CURRENT_TIMESTAMP), COALESCE(?, CURRENT_TIMESTAMP))";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getCode());
            ps.setString(2, product.getName());
            ps.setString(3, product.getCategory());
            ps.setString(4, product.getSupplier());
            ps.setString(5, product.getLocation());

            setNullableDouble(ps, 6, product.getPurchasePrice());
            setNullableDouble(ps, 7, product.getSalePrice());
            setNullableInt(ps, 8, product.getCurrentStock());
            setNullableInt(ps, 9, product.getMinStockAlert());

            LocalDateTime createdAt = product.getCreatedAt();
            if (createdAt != null) {
                ps.setTimestamp(10, Timestamp.valueOf(createdAt));
            } else {
                ps.setTimestamp(10, null);
            }

            LocalDateTime updatedAt = product.getUpdatedAt();
            if (updatedAt != null) {
                ps.setTimestamp(11, Timestamp.valueOf(updatedAt));
            } else {
                ps.setTimestamp(11, null);
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
        String sql = "UPDATE products SET code = ?, name = ?, category = ?, supplier = ?, location = ?, " +
                "purchase_price = ?, sale_price = ?, current_stock = ?, min_stock_alert = ?, " +
                "updated_at = COALESCE(?, CURRENT_TIMESTAMP) WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getCode());
            ps.setString(2, product.getName());
            ps.setString(3, product.getCategory());
            ps.setString(4, product.getSupplier());
            ps.setString(5, product.getLocation());

            setNullableDouble(ps, 6, product.getPurchasePrice());
            setNullableDouble(ps, 7, product.getSalePrice());
            setNullableInt(ps, 8, product.getCurrentStock());
            setNullableInt(ps, 9, product.getMinStockAlert());

            LocalDateTime updatedAt = product.getUpdatedAt();
            if (updatedAt != null) {
                ps.setTimestamp(10, Timestamp.valueOf(updatedAt));
            } else {
                ps.setTimestamp(10, null);
            }

            ps.setLong(11, product.getId());
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
        p.setCategory(rs.getString("category"));
        p.setSupplier(rs.getString("supplier"));
        p.setLocation(rs.getString("location"));

        p.setPurchasePrice(toNullableDouble(rs, "purchase_price"));
        p.setSalePrice(toNullableDouble(rs, "sale_price"));
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

    private static Double toNullableDouble(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        return o == null ? null : ((Number) o).doubleValue();
    }

    private static Integer toNullableInt(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        return o == null ? null : ((Number) o).intValue();
    }
}

