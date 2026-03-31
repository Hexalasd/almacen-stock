package com.almacen.dao;

import com.almacen.model.Alert;
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

public class AlertDAO {
    private static final Logger logger = LogManager.getLogger(AlertDAO.class);

    public List<Alert> findAllUnresolved() {
        String sql = "SELECT id, product_id, type, message, resolved, created_at FROM alerts WHERE resolved = 0 ORDER BY created_at ASC";
        List<Alert> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error executing findAllUnresolved", e);
        }

        return result;
    }

    public List<Alert> findByProductId(int productId) {
        String sql = "SELECT id, product_id, type, message, resolved, created_at FROM alerts WHERE product_id = ? ORDER BY created_at ASC";
        List<Alert> result = new ArrayList<>();

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

    public int save(Alert alert) {
        String sql = "INSERT INTO alerts (product_id, type, message, resolved, created_at) " +
                "VALUES (?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP))";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, alert.getProductId());
            ps.setString(2, alert.getType() != null ? alert.getType().name() : null);
            ps.setString(3, alert.getMessage());
            ps.setInt(4, alert.isResolved() ? 1 : 0);

            LocalDateTime createdAt = alert.getCreatedAt();
            if (createdAt != null) {
                ps.setTimestamp(5, Timestamp.valueOf(createdAt));
            } else {
                ps.setTimestamp(5, null);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing save alert={}", alert, e);
        }

        return 0;
    }

    public boolean resolve(int alertId) {
        String sql = "UPDATE alerts SET resolved = 1 WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, alertId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error executing resolve alertId={}", alertId, e);
            return false;
        }
    }

    private static Alert mapRow(ResultSet rs) throws SQLException {
        Alert a = new Alert();
        a.setId(rs.getLong("id"));
        a.setProductId(rs.getLong("product_id"));

        String type = rs.getString("type");
        if (type != null) {
            a.setType(Alert.AlertType.valueOf(type));
        }

        a.setMessage(rs.getString("message"));
        a.setResolved(rs.getInt("resolved") == 1);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            a.setCreatedAt(createdAt.toLocalDateTime());
        }

        return a;
    }
}

