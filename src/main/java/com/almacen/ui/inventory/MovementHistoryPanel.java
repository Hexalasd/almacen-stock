package com.almacen.ui.inventory;

import com.almacen.model.InventoryMovement;
import com.almacen.service.InventoryService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MovementHistoryPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger(MovementHistoryPanel.class);

    private final String productCode;

    private final List<InventoryMovement> allMovements;
    private final MovementTableModel tableModel;
    private final JTable table;

    private final JTextField startDateField;
    private final JTextField endDateField;
    private final JComboBox<Object> typeCombo;

    private final JButton applyButton;
    private final JButton exportButton;

    public MovementHistoryPanel(int productId, String productCode, String productName, InventoryService inventoryService) {
        this.productCode = productCode;

        setLayout(new BorderLayout(10, 10));

        // Cargar una vez al abrir
        this.allMovements = new ArrayList<>(inventoryService.getMovementHistory(productId));
        this.allMovements.sort(Comparator.comparing(InventoryMovement::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(InventoryMovement::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        JLabel title = new JLabel("Historial: " + safe(productCode) + " - " + safe(productName));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        tableModel = new MovementTableModel();
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Inicio (yyyy-MM-dd):"));
        startDateField = new JTextField(10);
        filters.add(startDateField);

        filters.add(new JLabel("Fin (yyyy-MM-dd):"));
        endDateField = new JTextField(10);
        filters.add(endDateField);

        filters.add(new JLabel("Tipo:"));
        typeCombo = new JComboBox<>();
        typeCombo.addItem("TODOS");
        for (InventoryMovement.MovementType t : InventoryMovement.MovementType.values()) {
            typeCombo.addItem(t);
        }
        typeCombo.setSelectedItem("TODOS");
        filters.add(typeCombo);

        applyButton = new JButton("Filtrar");
        exportButton = new JButton("Exportar CSV");
        filters.add(applyButton);
        filters.add(exportButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(title, BorderLayout.NORTH);
        top.add(filters, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        applyButton.addActionListener(e -> applyFilters());
        exportButton.addActionListener(e -> exportCsv());

        // mostrar todo por defecto
        tableModel.setMovements(new ArrayList<>(allMovements));
    }

    private void applyFilters() {
        LocalDate start = parseLocalDate(startDateField.getText());
        LocalDate end = parseLocalDate(endDateField.getText());
        Object selected = typeCombo.getSelectedItem();
        InventoryMovement.MovementType type = selected instanceof InventoryMovement.MovementType ? (InventoryMovement.MovementType) selected : null;

        LocalDateTime startDt = start == null ? null : start.atStartOfDay();
        LocalDateTime endDt = end == null ? null : end.atTime(LocalTime.MAX);

        List<InventoryMovement> filtered = new ArrayList<>();
        for (InventoryMovement mv : allMovements) {
            LocalDateTime ts = mv.getTimestamp();
            if (type != null && mv.getType() != type) continue;

            if (startDt != null) {
                if (ts == null || ts.isBefore(startDt)) continue;
            }
            if (endDt != null) {
                if (ts == null || ts.isAfter(endDt)) continue;
            }

            filtered.add(mv);
        }

        tableModel.setMovements(filtered);
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar historial a CSV");
        chooser.setSelectedFile(Path.of("movimientos-" + safe(productCode) + ".csv").toFile());

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path out = chooser.getSelectedFile().toPath();

        try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            writer.write("fecha,tipo,cantidad,motivo,usuario");
            writer.newLine();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (InventoryMovement mv : tableModel.getMovements()) {
                String fecha = mv.getTimestamp() == null ? "" : mv.getTimestamp().format(fmt);
                String tipo = mv.getType() == null ? "" : mv.getType().name();
                String cantidad = mv.getQuantity() == null ? "" : mv.getQuantity().toString();
                String motivo = csvEscape(mv.getReason());
                String usuario = mv.getUserId() == null ? "" : mv.getUserId().toString();

                writer.write(String.join(",",
                        quote(fecha),
                        quote(tipo),
                        quote(cantidad),
                        quote(motivo),
                        quote(usuario)
                ));
                writer.newLine();
            }

            logger.info("CSV exported to {}", out.toAbsolutePath());
            JOptionPane.showMessageDialog(this, "CSV exportado correctamente.", "CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            logger.error("Failed exporting CSV", e);
            JOptionPane.showMessageDialog(this, "No se pudo exportar el CSV.", "CSV", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static LocalDate parseLocalDate(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return null;
        try {
            return LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String csvEscape(String s) {
        // Dejamos el quote() como escapado principal; solo normalizamos nulos.
        return s == null ? "" : s;
    }

    private static String quote(String s) {
        String v = s == null ? "" : s;
        String escaped = v.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static class MovementTableModel extends AbstractTableModel {
        private final String[] columns = {"Fecha", "Tipo", "Cantidad", "Motivo", "Usuario"};
        private List<InventoryMovement> movements = new ArrayList<>();

        @Override
        public int getRowCount() {
            return movements.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        public void setMovements(List<InventoryMovement> newMovements) {
            this.movements = newMovements == null ? new ArrayList<>() : newMovements;
            fireTableDataChanged();
        }

        public List<InventoryMovement> getMovements() {
            return movements;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            InventoryMovement mv = movements.get(rowIndex);
            if (mv == null) return null;

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return switch (columnIndex) {
                case 0 -> mv.getTimestamp() == null ? "" : mv.getTimestamp().format(fmt);
                case 1 -> mv.getType() == null ? "" : mv.getType().name();
                case 2 -> mv.getQuantity() == null ? "" : mv.getQuantity();
                case 3 -> mv.getReason() == null ? "" : mv.getReason();
                case 4 -> mv.getUserId() == null ? "" : mv.getUserId();
                default -> null;
            };
        }
    }
}

