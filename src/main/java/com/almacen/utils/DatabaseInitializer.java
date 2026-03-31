package com.almacen.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class DatabaseInitializer {
    private static final Logger logger = LogManager.getLogger(DatabaseInitializer.class);
    private static final String SCHEMA_FILE = "schema.sql";

    private DatabaseInitializer() {
    }

    public static void initializeIfNeeded() {
        boolean dbFileExists = databaseFileExists();

        if (!dbFileExists) {
            logger.info("Database file not found. Initializing schema...");
            runSchema();
            return;
        }

        if (!hasUserTables()) {
            logger.info("Database has no tables. Creating schema...");
            runSchema();
        } else {
            logger.info("Database already initialized.");
        }
    }

    private static boolean databaseFileExists() {
        try {
            Properties props = DatabaseConnection.loadProperties();
            String url = props.getProperty("db.url", "").trim();

            String prefix = "jdbc:sqlite:";
            if (!url.startsWith(prefix)) {
                return false;
            }

            String pathPart = url.substring(prefix.length());
            if (pathPart.isBlank() || ":memory:".equalsIgnoreCase(pathPart.trim())) {
                return true;
            }

            Path dbPath = Paths.get(pathPart);
            if (!dbPath.isAbsolute()) {
                dbPath = Paths.get(System.getProperty("user.dir")).resolve(dbPath).normalize();
            }

            return Files.exists(dbPath);
        } catch (Exception e) {
            logger.warn("Could not determine whether database file exists.", e);
            return false;
        }
    }

    private static boolean hasUserTables() {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) {
            logger.error("Failed to check existing tables.", e);
            return false;
        }
    }

    private static void runSchema() {
        try {
            List<String> statements = loadSqlStatementsFromResource(SCHEMA_FILE);
            if (statements.isEmpty()) {
                logger.warn("No SQL statements found in {}", SCHEMA_FILE);
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection();
                 Statement st = conn.createStatement()) {
                for (String stmt : statements) {
                    st.execute(stmt);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialize database schema from {}", SCHEMA_FILE, e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static List<String> loadSqlStatementsFromResource(String resourceName) throws IOException {
        try (InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Could not find resource on classpath: " + resourceName);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                        continue;
                    }
                    sb.append(line).append('\n');
                }
            }

            return splitSqlStatements(sb.toString());
        }
    }

    private static List<String> splitSqlStatements(String sql) {
        String[] parts = sql.split(";");
        List<String> statements = new ArrayList<>();
        for (String part : parts) {
            String stmt = part.trim();
            if (!stmt.isEmpty()) {
                statements.add(stmt);
            }
        }
        return statements;
    }
}
