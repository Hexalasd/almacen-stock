package com.almacen.dao;

import com.almacen.model.User;
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

public class UserDAO {
    private static final Logger logger = LogManager.getLogger(UserDAO.class);

    public List<User> findAll() {
        String sql = "SELECT id, username, password, role, full_name, active, created_at FROM users ORDER BY id";
        List<User> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error executing findAll users", e);
        }

        return result;
    }

    public User findById(int id) {
        String sql = "SELECT id, username, password, role, full_name, active, created_at FROM users WHERE id = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing findById for id={}", id, e);
        }

        return null;
    }

    public User findByUsername(String username) {
        String sql = "SELECT id, username, password, role, full_name, active, created_at FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing findByUsername username={}", username, e);
        }

        return null;
    }

    public int save(User user) {
        String sql = "INSERT INTO users (username, password, role, full_name, active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP))";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole() != null ? user.getRole().name() : null);
            ps.setString(4, user.getFullName());
            ps.setInt(5, user.isActive() ? 1 : 0);
            LocalDateTime createdAt = user.getCreatedAt();
            if (createdAt != null) {
                ps.setTimestamp(6, Timestamp.valueOf(createdAt));
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
            logger.error("Error executing save user={}", user, e);
        }

        return 0;
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET username = ?, password = ?, role = ?, full_name = ?, active = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole() != null ? user.getRole().name() : null);
            ps.setString(4, user.getFullName());
            ps.setInt(5, user.isActive() ? 1 : 0);
            ps.setLong(6, user.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error executing update user={}", user, e);
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error executing delete user id={}", id, e);
            return false;
        }
    }

    public User authenticate(String username, String password) {
        String sql = "SELECT id, username, password, role, full_name, active, created_at " +
                "FROM users WHERE username = ? AND password = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing authenticate username={}", username, e);
        }

        return null;
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));

        String role = rs.getString("role");
        if (role != null) {
            u.setRole(User.Role.valueOf(role));
        }

        u.setFullName(rs.getString("full_name"));
        u.setActive(rs.getInt("active") == 1);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            u.setCreatedAt(createdAt.toLocalDateTime());
        }

        return u;
    }
}

