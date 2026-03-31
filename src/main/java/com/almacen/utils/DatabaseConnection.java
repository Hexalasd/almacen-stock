package com.almacen.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private static final Logger logger = LogManager.getLogger(DatabaseConnection.class);
    private static final String PROPERTIES_FILE = "database.properties";

    private static volatile Properties cachedProperties;

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Properties props = loadProperties();
        String url = props.getProperty("db.url");
        String driver = props.getProperty("db.driver");

        if (url == null || url.isBlank()) {
            throw new SQLException("Missing required property: db.url");
        }

        try {
            if (driver != null && !driver.isBlank()) {
                Class.forName(driver);
            }
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC driver class not found: {}", driver, e);
            throw new SQLException("SQLite JDBC driver class not found: " + driver, e);
        }

        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            logger.error("Failed to open SQLite connection to {}", url, e);
            throw e;
        }
    }

    public static Properties loadProperties() throws SQLException {
        Properties existing = cachedProperties;
        if (existing != null) {
            return existing;
        }

        synchronized (DatabaseConnection.class) {
            if (cachedProperties != null) {
                return cachedProperties;
            }

            Properties props = new Properties();
            try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
                if (in == null) {
                    throw new SQLException("Could not find " + PROPERTIES_FILE + " on the classpath");
                }
                props.load(in);
            } catch (IOException e) {
                logger.error("Failed to read {}", PROPERTIES_FILE, e);
                throw new SQLException("Failed to read " + PROPERTIES_FILE, e);
            }

            cachedProperties = props;
            return props;
        }
    }
}
