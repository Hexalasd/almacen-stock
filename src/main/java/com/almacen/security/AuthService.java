package com.almacen.security;

import com.almacen.dao.UserDAO;
import com.almacen.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthService {
    private static final Logger logger = LogManager.getLogger(AuthService.class);

    private final UserDAO userDAO;
    private volatile User currentUser;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return null;
        }

        User user = userDAO.findByUsername(username);
        if (user == null) {
            logger.warn("Login failed: user not found username={}", username);
            return null;
        }
        if (!user.isActive()) {
            logger.warn("Login failed: inactive user username={}", username);
            return null;
        }

        String stored = user.getPassword();
        boolean ok;
        if (stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"))) {
            ok = PasswordUtils.verifyPassword(password, stored);
        } else {
            ok = password.equals(stored);
        }

        if (!ok) {
            logger.warn("Login failed: invalid password username={}", username);
            return null;
        }

        currentUser = user;
        logger.info("Login success username={} role={}", user.getUsername(), user.getRole());
        return user;
    }

    public boolean hasPermission(User user, String requiredRole) {
        if (user == null || !user.isActive()) {
            return false;
        }
        if (requiredRole == null || requiredRole.isBlank()) {
            return true;
        }

        User.Role required;
        try {
            required = User.Role.valueOf(requiredRole.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown requiredRole={}", requiredRole);
            return false;
        }

        User.Role actual = user.getRole();
        if (actual == null) return false;
        if (actual == User.Role.ADMIN) return true;
        return actual == required;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        User u = currentUser;
        currentUser = null;
        if (u != null) {
            logger.info("Logout username={}", u.getUsername());
        }
    }

    public boolean changePassword(int userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("newPassword is required");
        }

        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("changePassword failed: user not found userId={}", userId);
            return false;
        }

        String hashed = PasswordUtils.hashPassword(newPassword);
        user.setPassword(hashed);

        boolean ok = userDAO.update(user);
        if (ok) {
            logger.info("Password changed userId={}", userId);
            if (currentUser != null && currentUser.getId() != null && currentUser.getId().equals(user.getId())) {
                currentUser.setPassword(hashed);
            }
        } else {
            logger.error("Password change failed userId={}", userId);
        }
        return ok;
    }
}

