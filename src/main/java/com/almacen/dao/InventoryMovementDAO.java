package com.almacen.dao;

import com.almacen.model.InventoryMovement;
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

public class InventoryMovementDAO {
    private static final Logger logger = LogManager.getLogger(InventoryMovementDAO.class);

    public List<InventoryMovement> findAll() {
        String sql = "SELECT id, product_id, quantity, type, reason, user_id, timestamp " +
                "FROM inventory_movements ORDER BY timestamp ASC";

        List<InventoryMovement> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error executing findAll inventory movements", e);
        }

        return result;
    }

    public List<InventoryMovement> findByProductId(int productId) {
        String sql = "SELECT id, product_id, quantity, type, reason, user_id, timestamp " +
                "FROM inventory_movements WHERE product_id = ? ORDER BY timestamp ASC";

        List<InventoryMovement> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing findByProductId productId={}", productId, e);
        }

        return result;
    }

    public List<InventoryMovement> findByDateRange(String startDate, String endDate) {
        String sql = "SELECT id, product_id, quantity, type, reason, user_id, timestamp " +
                "FROM inventory_movements WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp ASC";

        List<InventoryMovement> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing findByDateRange startDate={} endDate={}", startDate, endDate, e);
        }

        return result;
    }

    public int save(InventoryMovement movement) {
        String sql = "INSERT INTO inventory_movements (product_id, quantity, type, reason, user_id, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP))";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, movement.getProductId());
            ps.setInt(2, movement.getQuantity());
            ps.setString(3, movement.getType() != null ? movement.getType().name() : null);
            ps.setString(4, movement.getReason());

            if (movement.getUserId() != null) {
                ps.setLong(5, movement.getUserId());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }

            LocalDateTime ts = movement.getTimestamp();
            if (ts != null) {
                ps.setTimestamp(6, Timestamp.valueOf(ts));
            } else {
                ps.setTimestamp(6, null);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing save inventory movement={}", movement, e);
        }

        return 0;
    }

    public int getCurrentStock(int productId) {
        String sql = "SELECT IFNULL(SUM(CASE type " +
                "WHEN 'ENTRY' THEN quantity " +
                "WHEN 'EXIT' THEN -quantity " +
                "WHEN 'ADJUSTMENT' THEN quantity " +
                "ELSE 0 END), 0) AS stock " +
                "FROM inventory_movements WHERE product_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock");
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing getCurrentStock productId={}", productId, e);
        }

        return 0;
    }

    private static InventoryMovement mapRow(ResultSet rs) throws SQLException {
        InventoryMovement m = new InventoryMovement();
        m.setId(rs.getLong("id"));
        m.setProductId(rs.getLong("product_id"));
        m.setQuantity(rs.getInt("quantity"));

        String type = rs.getString("type");
        if (type != null) {
            m.setType(InventoryMovement.MovementType.valueOf(type));
        }

        m.setReason(rs.getString("reason"));

        Object userIdObj = rs.getObject("user_id");
        if (userIdObj != null) {
            m.setUserId(((Number) userIdObj).longValue());
        }

        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) {
            m.setTimestamp(ts.toLocalDateTime());
        }

        return m;
    }
}

