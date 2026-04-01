package com.almacen.ui.inventory;

import com.almacen.events.StockEventManager;
import com.almacen.events.StockUpdateListener;
import com.almacen.model.InventoryMovement;
import com.almacen.model.Product;
import com.almacen.service.InventoryService;
import com.almacen.service.ProductService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProductPanel extends JPanel implements StockUpdateListener {
    private static final Logger logger = LogManager.getLogger(ProductPanel.class);

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final StockEventManager eventManager;

    private final ProductTableModel tableModel;
    private final JTable table;

    private final JTextField searchField;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JButton movementsButton;
    private final JButton refreshButton;

    public ProductPanel() {
        this.productService = new ProductService();
        this.inventoryService = new InventoryService();
        this.eventManager = StockEventManager.getInstance();

        this.tableModel = new ProductTableModel();
        this.table = new JTable(tableModel);
        this.table.setFillsViewportHeight(true);
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.table.setRowHeight(24);

        // Renderizador para pintar filas con alerta (stock < minStockAlert)
        TableCellRenderer base = new DefaultTableCellRenderer();
        this.table.setDefaultRenderer(Object.class, new AlertRowRenderer(base));

        // Tooltips por fila
        this.table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    Product p = tableModel.getProductAt(row);
                    if (p != null) {
                        table.setToolTipText(buildTooltip(p));
                    } else {
                        table.setToolTipText(null);
                    }
                }
            }
        });

        searchField = new JTextField(25);
        addButton = new JButton("Agregar");
        editButton = new JButton("Editar");
        deleteButton = new JButton("Eliminar");
        movementsButton = new JButton("Ver Movimientos");
        refreshButton = new JButton("Refrescar (F5)");
        refreshButton.setToolTipText("Refrescar lista de productos (Ctrl+R)");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Buscar:"));
        top.add(searchField);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(refreshButton);
        bottom.add(movementsButton);
        bottom.add(deleteButton);
        bottom.add(editButton);
        bottom.add(addButton);

        setLayout(new BorderLayout(10, 10));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Filtro en tiempo real
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshFilter(); }
        });

        // Dobles click -> edición
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        editSelected(row);
                    }
                }
            }
        });

        addButton.addActionListener(e -> openAdd());
        editButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) editSelected(row);
        });
        deleteButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) deleteSelected(row);
        });
        movementsButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) showMovements(row);
        });
        refreshButton.addActionListener(e -> refreshAll());
        
        // Configurar shortcut Ctrl+R para refrescar
        setupRefreshShortcut();

        refreshAll();
        
        // Registrar como listener de eventos de stock
        eventManager.addListener(this);
    }

    private void refreshFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        if (keyword.isBlank()) {
            refreshAll();
            return;
        }
        List<Product> filtered = productService.searchProducts(keyword);
        tableModel.setProducts(filtered);
    }

    private void refreshAll() {
        List<Product> all = productService.getAllProducts();
        tableModel.setProducts(all);
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
            public void actionPerformed(ActionEvent e) {
                refreshAll();
            }
        });
        
        // F5
        KeyStroke f5 = KeyStroke.getKeyStroke("F5");
        inputMap.put(f5, "refreshF5");
        actionMap.put("refreshF5", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshAll();
            }
        });
    }

    private Product getSelectedProduct(int row) {
        return tableModel.getProductAt(row);
    }

    private void openAdd() {
        Window w = SwingUtilities.getWindowAncestor(this);
        ProductDialog dialog = new ProductDialog(w instanceof Frame ? (Frame) w : null, "Agregar Producto", productService, null);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            logger.info("Product added.");
            refreshAll();
            // Notificar que se agregó un nuevo producto (obtener ID del producto guardado)
            // Por ahora notificamos como actualización general
            eventManager.notifyAllProductsChanged();
        }
    }

    private void editSelected(int row) {
        Product p = getSelectedProduct(row);
        if (p == null) return;

        Window w = SwingUtilities.getWindowAncestor(this);
        ProductDialog dialog = new ProductDialog(w instanceof Frame ? (Frame) w : null, "Editar Producto", productService, p);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            logger.info("Product updated id={}", p.getId());
            refreshAll();
        }
    }

    private void deleteSelected(int row) {
        Product p = getSelectedProduct(row);
        if (p == null || p.getId() == null) return;

        int option = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar el producto " + safe(p.getCode()) + "?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (option != JOptionPane.YES_OPTION) return;

        boolean ok = productService.deleteProduct(p.getId().intValue());
        if (ok) {
            logger.warn("Product deleted id={}", p.getId());
            refreshAll();
            // Notificar que se eliminó un producto
            eventManager.notifyProductDeleted(p.getId());
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar el producto.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMovements(int row) {
        Product p = getSelectedProduct(row);
        if (p == null || p.getId() == null) return;

        List<InventoryMovement> history = inventoryService.getMovementHistory(p.getId().intValue());
        StringBuilder sb = new StringBuilder();
        sb.append("Movimientos para ").append(safe(p.getCode())).append(" - ").append(safe(p.getName())).append("\n\n");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (InventoryMovement mv : history) {
            sb.append("- [")
                    .append(mv.getType())
                    .append("] qty=")
                    .append(mv.getQuantity())
                    .append(" razon=")
                    .append(safe(mv.getReason()))
                    .append(" fecha=")
                    .append(mv.getTimestamp() == null ? "-" : mv.getTimestamp().format(fmt))
                    .append("\n");
        }

        if (history.isEmpty()) {
            sb.append("Sin movimientos aún.");
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setCaretPosition(0);

        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Movimientos", JOptionPane.INFORMATION_MESSAGE);
    }

    private String buildTooltip(Product p) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width:220px'>");
        sb.append("<b>").append(escapeHtml(safe(p.getName()))).append("</b><br/>");
        sb.append("Código: ").append(escapeHtml(safe(p.getCode()))).append("<br/>");
        if (p.getSupplier() != null && !p.getSupplier().isBlank()) {
            sb.append("Proveedor: ").append(escapeHtml(p.getSupplier())).append("<br/>");
        }
        if (p.getLocation() != null && !p.getLocation().isBlank()) {
            sb.append("Ubicación: ").append(escapeHtml(p.getLocation())).append("<br/>");
        }
        sb.append("Stock: ").append(p.getCurrentStock() == null ? 0 : p.getCurrentStock()).append("<br/>");
        sb.append("Mín alerta: ").append(p.getMinStockAlert() == null ? 0 : p.getMinStockAlert());
        sb.append("</body></html>");
        return sb.toString();
    }

    @Override
    public void onStockUpdated(Long productId) {
        // Refrescar la tabla para mostrar el stock actualizado
        SwingUtilities.invokeLater(() -> {
            if (productId != null) {
                // Actualizar solo el producto específico si es posible
                refreshSpecificProduct(productId);
            } else {
                // Refrescar toda la tabla
                refreshAll();
            }
        });
    }
    
    @Override
    public void onProductAdded(Long productId) {
        // Refrescar la tabla para mostrar el nuevo producto
        SwingUtilities.invokeLater(this::refreshAll);
    }
    
    @Override
    public void onProductDeleted(Long productId) {
        // Refrescar la tabla para eliminar el producto
        SwingUtilities.invokeLater(this::refreshAll);
    }
    
    private void refreshSpecificProduct(Long productId) {
        // Buscar el producto en la tabla y actualizar solo esa fila
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Product p = tableModel.getProductAt(i);
            if (p != null && productId.equals(p.getId())) {
                // Obtener datos actualizados del producto
                Product updated = productService.getProductByCode(p.getCode());
                if (updated != null) {
                    // Actualizar el producto en el modelo
                    tableModel.products.set(i, updated);
                    tableModel.fireTableRowsUpdated(i, i);
                }
                break;
            }
        }
    }
    
    // Método para limpiar el listener cuando el panel se destruye
    public void cleanup() {
        eventManager.removeListener(this);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
                } else {
                    if (!isSelected) {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                }
            }
            return c;
        }
    }

    private static class ProductTableModel extends AbstractTableModel {
        private final String[] columns = {"Código", "Nombre", "Categoría", "Stock", "Precio Venta", "Alerta"};
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

        public Product getProductAt(int row) {
            if (row < 0 || row >= products.size()) return null;
            return products.get(row);
        }

        public void setProducts(List<Product> newProducts) {
            this.products = newProducts == null ? new ArrayList<>() : newProducts;
            fireTableDataChanged();
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
                case 4 -> p.getSalePriceWithUnit();
                case 5 -> low ? "Bajo" : "OK";
                default -> null;
            };
        }
    }
}

