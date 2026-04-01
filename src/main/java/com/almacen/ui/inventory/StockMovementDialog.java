package com.almacen.ui.inventory;

import com.almacen.model.Product;
import com.almacen.service.InventoryService;
import com.almacen.service.ProductService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.function.IntSupplier;

public class StockMovementDialog extends JDialog {
    private static final Logger logger = LogManager.getLogger(StockMovementDialog.class);

    public enum Action { ADD, REMOVE, ADJUST }

    private final Action action;
    private final InventoryService inventoryService;
    private final ProductService productService;
    private final IntSupplier currentUserIdSupplier;

    private boolean saved;

    private final JComboBox<Product> productCombo;
    private final JSpinner quantitySpinner;
    private final JTextField reasonField;
    private final JButton confirmButton;
    private final JButton cancelButton;

    public StockMovementDialog(
            Window parent,
            Action action,
            Product preselectedProduct,
            ProductService productService,
            InventoryService inventoryService,
            IntSupplier currentUserIdSupplier
    ) {
        super(parent instanceof Frame ? (Frame) parent : null, dialogTitle(action), true);

        if (action == null) throw new IllegalArgumentException("action cannot be null");
        this.action = action;
        this.inventoryService = inventoryService;
        this.currentUserIdSupplier = currentUserIdSupplier;
        this.productService = productService;

        setLayout(new BorderLayout(10, 10));

        productCombo = new JComboBox<>();
        productCombo.setEditable(true);
        productCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product p) {
                    setText(p.getCode() + " - " + (p.getName() == null ? "" : p.getName()));
                }
                return this;
            }
        });

        refreshComboItems("");
        if (preselectedProduct != null && preselectedProduct.getId() != null) {
            productCombo.setSelectedItem(preselectedProduct);
        }

        // Buscar mientras escribe
        Object editorComp = productCombo.getEditor().getEditorComponent();
        if (editorComp instanceof JTextField editor) {
            editor.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { refreshComboFromEditor(); }
                @Override public void removeUpdate(DocumentEvent e) { refreshComboFromEditor(); }
                @Override public void changedUpdate(DocumentEvent e) { refreshComboFromEditor(); }
                private void refreshComboFromEditor() {
                    refreshComboItems(editor.getText());
                }
            });
        }

        quantitySpinner = new JSpinner(new SpinnerNumberModel(
                action == Action.ADJUST ? 0 : 1,
                0,
                Integer.MAX_VALUE,
                1
        ));

        reasonField = new JTextField(25);

        confirmButton = new JButton("Confirmar");
        cancelButton = new JButton("Cancelar");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, c, new JLabel("Producto:"), productCombo, 0);
        addRow(form, c, new JLabel(quantityLabel(action) + ":"), quantitySpinner, 1);
        addRow(form, c, new JLabel("Motivo (texto libre):"), reasonField, 2);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(confirmButton);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(520, 260));

        confirmButton.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(confirmButton);
        pack();
    }

    private void refreshComboItems(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        productCombo.removeAllItems();
        
        // Siempre obtener datos frescos desde la base de datos
        List<Product> freshProducts = productService.getAllProducts();
        
        for (Product p : freshProducts) {
            String code = p.getCode() == null ? "" : p.getCode().toLowerCase();
            String name = p.getName() == null ? "" : p.getName().toLowerCase();
            if (k.isEmpty() || code.contains(k) || name.contains(k)) {
                productCombo.addItem(p);
            }
        }

        // Mantener selección si el usuario escribió y todavía existe el item
        if (productCombo.getItemCount() > 0 && productCombo.getSelectedIndex() < 0) {
            productCombo.setSelectedIndex(0);
        }
    }

    private void onConfirm() {
        Product selected = (Product) productCombo.getSelectedItem();
        if (selected == null || selected.getId() == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto válido.", "Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int userId;
        try {
            userId = currentUserIdSupplier.getAsInt();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Necesitas iniciar sesión.", "Stock", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int qty = ((Number) quantitySpinner.getValue()).intValue();
        String reason = reasonField.getText() == null ? "" : reasonField.getText().trim();

        boolean ok;
        try {
            int productId = selected.getId().intValue();
            if (action == Action.ADD) {
                if (qty <= 0) {
                    JOptionPane.showMessageDialog(this, "La cantidad debe ser > 0.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ok = inventoryService.addStock(productId, qty, reason, userId);
            } else if (action == Action.REMOVE) {
                if (qty <= 0) {
                    JOptionPane.showMessageDialog(this, "La cantidad debe ser > 0.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ok = inventoryService.removeStock(productId, qty, reason, userId);
            } else {
                // ADJUST: qty representa nuevo stock
                ok = inventoryService.adjustStock(productId, qty, reason, userId);
            }
        } catch (IllegalStateException ex) {
            logger.warn("Stock operation rejected", ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Stock", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (Exception ex) {
            logger.error("Unexpected error in StockMovementDialog", ex);
            JOptionPane.showMessageDialog(this, "Ocurrió un error procesando la operación.", "Stock", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo completar la operación.", "Stock", JOptionPane.ERROR_MESSAGE);
            return;
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }

    private static String dialogTitle(Action action) {
        return switch (action) {
            case ADD -> "Agregar Stock";
            case REMOVE -> "Quitar Stock";
            case ADJUST -> "Ajustar Stock";
        };
    }

    private static String quantityLabel(Action action) {
        return switch (action) {
            case ADD -> "Cantidad (entrada)";
            case REMOVE -> "Cantidad (salida)";
            case ADJUST -> "Nuevo stock";
        };
    }

    private static void addRow(JPanel form, GridBagConstraints base, JLabel label, JComponent comp, int row) {
        GridBagConstraints c = (GridBagConstraints) base.clone();
        c.gridy = row;

        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(label, c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(comp, c);
    }
}

