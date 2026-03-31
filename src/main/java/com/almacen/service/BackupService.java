package com.almacen.service;

import com.almacen.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class BackupService {
    private static final Logger logger = LogManager.getLogger(BackupService.class);

    private final Timer timer;

    public BackupService() {
        this.timer = new Timer("backup-timer", true);
    }

    public boolean createBackup(String filePath) {
        Path out = Paths.get(filePath);
        try {
            Path parent = out.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            logger.error("Failed to create backup directory for {}", filePath, e);
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {

            writer.write("-- Backup generado: " + LocalDateTime.now());
            writer.newLine();
            writer.write("PRAGMA foreign_keys = OFF;");
            writer.newLine();
            writer.write("BEGIN TRANSACTION;");
            writer.newLine();
            writer.newLine();

            List<String> tables = listUserTables(conn);
            for (String table : tables) {
                dumpTableSchema(conn, table, writer);
            }
            writer.newLine();

            for (String table : tables) {
                dumpTableData(conn, table, writer);
            }

            writer.newLine();
            writer.write("COMMIT;");
            writer.newLine();
            writer.write("PRAGMA foreign_keys = ON;");
            writer.newLine();

            logger.info("Backup created at {}", out.toAbsolutePath());
            return true;
        } catch (SQLException | IOException e) {
            logger.error("Failed to create backup at {}", filePath, e);
            return false;
        }
    }

    public boolean restoreBackup(String filePath) {
        Path in = Paths.get(filePath);
        if (!Files.exists(in)) {
            logger.error("Backup file not found: {}", filePath);
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = OFF");
            }

            List<String> statements = readSqlStatements(in);
            try (Statement st = conn.createStatement()) {
                for (String sql : statements) {
                    st.execute(sql);
                }
            }

            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            conn.commit();
            logger.warn("Backup restored from {}", in.toAbsolutePath());
            return true;
        } catch (Exception e) {
            logger.error("Failed to restore backup from {}", filePath, e);
            return false;
        }
    }

    public TimerTask scheduleDailyBackup(String backupFolder) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    String fileName = "backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".sql";
                    Path out = Paths.get(backupFolder).resolve(fileName);
                    boolean ok = createBackup(out.toString());
                    if (!ok) {
                        logger.error("Scheduled backup failed: {}", out);
                    }
                } catch (Exception e) {
                    logger.error("Scheduled backup task failed", e);
                }
            }
        };

        long delayMs = 0L;
        long periodMs = 24L * 60L * 60L * 1000L;
        timer.scheduleAtFixedRate(task, delayMs, periodMs);
        logger.info("Daily backup scheduled every 24h to folder={}", backupFolder);
        return task;
    }

    public List<String> getBackupList() {
        Path folder;
        try {
            Properties props = DatabaseConnection.loadProperties();
            folder = defaultBackupFolderFromProperties(props);
        } catch (Exception e) {
            folder = Paths.get("backups");
        }

        return getBackupList(folder.toString());
    }

    public List<String> getBackupList(String backupFolder) {
        Path folder = Paths.get(backupFolder);
        if (!Files.isDirectory(folder)) {
            return List.of();
        }

        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder, "*.sql")) {
            for (Path p : ds) {
                files.add(p);
            }
        } catch (IOException e) {
            logger.error("Failed to list backups in {}", backupFolder, e);
            return List.of();
        }

        files.sort(Comparator.comparing(Path::getFileName).reversed());
        List<String> result = new ArrayList<>();
        for (Path p : files) {
            result.add(p.toString());
        }
        return result;
    }

    private static Path defaultBackupFolderFromProperties(Properties props) {
        // Default: backups/ next to the sqlite db file (or cwd if unknown)
        String url = props.getProperty("db.url", "").trim();
        String prefix = "jdbc:sqlite:";
        if (url.startsWith(prefix)) {
            String pathPart = url.substring(prefix.length());
            if (!pathPart.isBlank() && !":memory:".equalsIgnoreCase(pathPart.trim())) {
                Path db = Paths.get(pathPart);
                if (!db.isAbsolute()) {
                    db = Paths.get(System.getProperty("user.dir")).resolve(db).normalize();
                }
                Path parent = db.getParent();
                if (parent != null) {
                    return parent.resolve("backups");
                }
            }
        }
        return Paths.get("backups");
    }

    private static List<String> listUserTables(Connection conn) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name";
        List<String> tables = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    private static void dumpTableSchema(Connection conn, String table, BufferedWriter writer) throws SQLException, IOException {
        String sql = "SELECT sql FROM sqlite_master WHERE type='table' AND name=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String ddl = rs.getString(1);
                    if (ddl != null && !ddl.isBlank()) {
                        writer.write("-- Schema for table: " + table);
                        writer.newLine();
                        writer.write("DROP TABLE IF EXISTS \"" + table + "\";");
                        writer.newLine();
                        writer.write(ddl.trim() + ";");
                        writer.newLine();
                        writer.newLine();
                    }
                }
            }
        }
    }

    private static void dumpTableData(Connection conn, String table, BufferedWriter writer) throws SQLException, IOException {
        String sql = "SELECT * FROM \"" + table.replace("\"", "\"\"") + "\"";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            int colCount = md.getColumnCount();

            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                sb.append("INSERT INTO \"").append(table.replace("\"", "\"\"")).append("\" (");
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) sb.append(", ");
                    sb.append("\"").append(md.getColumnName(i).replace("\"", "\"\"")).append("\"");
                }
                sb.append(") VALUES (");
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) sb.append(", ");
                    sb.append(toSqlLiteral(rs.getObject(i)));
                }
                sb.append(");");

                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    private static String toSqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            StringBuilder sb = new StringBuilder("X'");
            for (byte b : bytes) {
                sb.append(String.format("%02X", b));
            }
            sb.append("'");
            return sb.toString();
        }
        String s = String.valueOf(value);
        return "'" + s.replace("'", "''") + "'";
    }

    private static List<String> readSqlStatements(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                sb.append(line).append('\n');
            }
        }
        return splitSqlStatementsRespectingQuotes(sb.toString());
    }

    private static List<String> splitSqlStatementsRespectingQuotes(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSingleQuote = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            cur.append(c);

            if (c == '\'') {
                // Handle escaped '' inside strings
                char next = (i + 1 < sql.length()) ? sql.charAt(i + 1) : '\0';
                if (inSingleQuote && next == '\'') {
                    cur.append(next);
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
            }

            if (c == ';' && !inSingleQuote) {
                String stmt = cur.toString().trim();
                if (!stmt.isEmpty()) {
                    out.add(stmt.substring(0, stmt.length() - 1).trim()); // remove trailing ;
                }
                cur.setLength(0);
            }
        }

        String tail = cur.toString().trim();
        if (!tail.isEmpty()) {
            out.add(tail);
        }

        return out;
    }
}

