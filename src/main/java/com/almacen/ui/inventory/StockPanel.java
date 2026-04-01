package com.almacen.ui.inventory;

import com.almacen.events.StockEventManager;
import com.almacen.model.Product;
import com.almacen.security.AuthService;
import com.almacen.service.InventoryService;
import com.almacen.service.ProductService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class StockPanel extends JPanel {
    private final AuthService authService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final StockEventManager eventManager;

    private final StockTableModel tableModel;
    private final JTable table;

    private final JButton addStockButton;
    private final JButton removeStockButton;
    private final JButton adjustStockButton;
    private final JButton historyButton;
    private final JButton refreshButton;

    public StockPanel(AuthService authService) {
        this.authService = authService;
        this.productService = new ProductService();
        this.inventoryService = new InventoryService();
        this.eventManager = StockEventManager.getInstance();

        this.tableModel = new StockTableModel();
        this.table = new JTable(tableModel);
        this.table.setFillsViewportHeight(true);
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.table.setRowHeight(24);

        TableCellRenderer base = new DefaultTableCellRenderer();
        this.table.setDefaultRenderer(Object.class, new AlertRowRenderer(base));

        this.table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // doble click = refrescar selección y mostrar historial si se quiere en el futuro
            }
        });

        JScrollPane scroll = new JScrollPane(table);

        addStockButton = new JButton("Agregar Stock");
        removeStockButton = new JButton("Quitar Stock");
        adjustStockButton = new JButton("Ajustar Stock");
        historyButton = new JButton("Ver Historial");
        refreshButton = new JButton("Refrescar (F5)");
        refreshButton.setToolTipText("Refrescar lista de productos (Ctrl+R)");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(refreshButton);
        actions.add(historyButton);
        actions.add(adjustStockButton);
        actions.add(removeStockButton);
        actions.add(addStockButton);

        setLayout(new BorderLayout(10, 10));
        add(scroll, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        addStockButton.addActionListener(e -> openMovementDialog(StockMovementDialog.Action.ADD));
        removeStockButton.addActionListener(e -> openMovementDialog(StockMovementDialog.Action.REMOVE));
        adjustStockButton.addActionListener(e -> openMovementDialog(StockMovementDialog.Action.ADJUST));
        historyButton.addActionListener(e -> openHistoryPanel());
        refreshButton.addActionListener(e -> refresh());
        
        // Configurar shortcut Ctrl+R para refrescar
        setupRefreshShortcut();
        
        refresh();
    }

    private void refresh() {
        List<Product> products = productService.getAllProducts();
        tableModel.setProducts(products);
    }
    
    private void setupRefreshShortcut() {
        // Configurar shortcut Ctrl+R y F5 para refrescar
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        
        // Ctrl+R
        KeyStroke ctrlR = KeyStroke.getKeyStroke("ctrl R");
        inputMap.put(ctrlR, "refresh");
        actionMap.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                refresh();
            }
        });
        
        // F5
        KeyStroke f5 = KeyStroke.getKeyStroke("F5");
        inputMap.put(f5, "refreshF5");
        actionMap.put("refreshF5", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                refresh();
            }
        });
    }

    private Product selectedProduct() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getProductAt(row);
    }

    private int currentUserIdOrThrow() {
        if (authService == null || authService.getCurrentUser() == null || authService.getCurrentUser().getId() == null) {
            throw new IllegalStateException("No hay usuario autenticado.");
        }
        return authService.getCurrentUser().getId().intValue();
    }

    private void openMovementDialog(StockMovementDialog.Action action) {
        Product p = selectedProduct();
        if (p == null || p.getId() == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto primero.", "Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Window parent = SwingUtilities.getWindowAncestor(this);
        StockMovementDialog dialog = new StockMovementDialog(
                parent,
                action,
                p,
                productService,
                inventoryService,
                () -> currentUserIdOrThrow()
        );

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            refresh();
            // Notificar que el stock fue actualizado
            if (p != null && p.getId() != null) {
                eventManager.notifyStockUpdated(p.getId());
            }
        }
    }

    private void openHistoryPanel() {
        Product p = selectedProduct();
        if (p == null || p.getId() == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto primero.", "Historial", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Window parent = SwingUtilities.getWindowAncestor(this);
        MovementHistoryPanel panel = new MovementHistoryPanel(
                (int) p.getId().intValue(),
                p.getCode(),
                p.getName(),
                inventoryService
        );

        JDialog dlg = new JDialog(parent instanceof Frame ? (Frame) parent : null, "Historial de movimientos", true);
        dlg.setContentPane(panel);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private static class StockTableModel extends AbstractTableModel {
        private final String[] columns = {"Código", "Nombre", "Categoría", "Stock", "Alerta"};
        private List<Product> products = new ArrayList<>();

        @Override
        public int getRowCount() {
            return products.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        public void setProducts(List<Product> newProducts) {
            this.products = newProducts == null ? new ArrayList<>() : newProducts;
            fireTableDataChanged();
        }

        public Product getProductAt(int row) {
            if (row < 0 || row >= products.size()) return null;
            return products.get(row);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Product p = getProductAt(rowIndex);
            if (p == null) return null;

            Integer stock = p.getCurrentStock();
            Integer min = p.getMinStockAlert();
            boolean low = stock != null && min != null && stock < min;

            return switch (columnIndex) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getCategoryDisplay();
                case 3 -> stock == null ? 0 : stock;
                case 4 -> low ? "Bajo" : "OK";
                default -> null;
            };
        }
    }

    private class AlertRowRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;

        AlertRowRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            Product p = tableModel.getProductAt(row);
            if (p != null) {
                Integer stock = p.getCurrentStock();
                Integer min = p.getMinStockAlert();
                boolean low = stock != null && min != null && stock < min;

                if (low && !isSelected) {
                    c.setBackground(new Color(255, 200, 200));
                    c.setForeground(Color.BLACK);
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }

            return c;
        }
    }
}

