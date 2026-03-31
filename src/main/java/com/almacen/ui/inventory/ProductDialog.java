package com.almacen.ui.inventory;

import com.almacen.model.Product;
import com.almacen.service.ProductService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class ProductDialog extends JDialog {
    private static final Logger logger = LogManager.getLogger(ProductDialog.class);

    private final ProductService productService;
    private final Product editingProduct; // null => create

    private boolean saved;

    private final JTextField codeField;
    private final JTextField nameField;
    private final JTextField categoryField;
    private final JTextField supplierField;
    private final JTextField locationField;
    private final JTextField purchasePriceField;
    private final JTextField salePriceField;
    private final JTextField minStockAlertField;

    private final JButton saveButton;
    private final JButton cancelButton;

    public ProductDialog(Frame parent, String title, ProductService productService, Product editingProduct) {
        super(parent, title, true);
        if (productService == null) throw new IllegalArgumentException("productService cannot be null");

        this.productService = productService;
        this.editingProduct = editingProduct;

        JLabel codeLabel = new JLabel("Código:");
        JLabel nameLabel = new JLabel("Nombre:");
        JLabel categoryLabel = new JLabel("Categoría:");
        JLabel supplierLabel = new JLabel("Proveedor:");
        JLabel locationLabel = new JLabel("Ubicación:");
        JLabel purchaseLabel = new JLabel("Precio Compra:");
        JLabel saleLabel = new JLabel("Precio Venta:");
        JLabel minLabel = new JLabel("Stock Mínimo:");

        codeField = new JTextField(22);
        nameField = new JTextField(22);
        categoryField = new JTextField(22);
        supplierField = new JTextField(22);
        locationField = new JTextField(22);
        purchasePriceField = new JTextField(22);
        salePriceField = new JTextField(22);
        minStockAlertField = new JTextField(22);

        if (editingProduct != null) {
            codeField.setText(nvl(editingProduct.getCode()));
            nameField.setText(nvl(editingProduct.getName()));
            categoryField.setText(nvl(editingProduct.getCategory()));
            supplierField.setText(nvl(editingProduct.getSupplier()));
            locationField.setText(nvl(editingProduct.getLocation()));
            purchasePriceField.setText(editingProduct.getPurchasePrice() == null ? "" : editingProduct.getPurchasePrice().toString());
            salePriceField.setText(editingProduct.getSalePrice() == null ? "" : editingProduct.getSalePrice().toString());
            minStockAlertField.setText(editingProduct.getMinStockAlert() == null ? "" : editingProduct.getMinStockAlert().toString());
        }

        saveButton = new JButton("Guardar");
        cancelButton = new JButton("Cancelar");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, c, codeLabel, codeField, 0);
        addRow(form, c, nameLabel, nameField, 1);
        addRow(form, c, categoryLabel, categoryField, 2);
        addRow(form, c, supplierLabel, supplierField, 3);
        addRow(form, c, locationLabel, locationField, 4);
        addRow(form, c, purchaseLabel, purchasePriceField, 5);
        addRow(form, c, saleLabel, salePriceField, 6);
        addRow(form, c, minLabel, minStockAlertField, 7);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        setLayout(new BorderLayout(10, 10));
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(520, 420));
        setResizable(false);

        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(saveButton);

        pack();
        setLocationRelativeTo(parent);
    }

    private void onSave() {
        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        if (code.isBlank()) {
            JOptionPane.showMessageDialog(this, "El código es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            codeField.requestFocusInWindow();
            return;
        }

        // Unique code validation
        Product existingByCode = productService.getProductByCode(code);
        if (editingProduct == null) {
            if (existingByCode != null) {
                JOptionPane.showMessageDialog(this, "El código ya existe: " + code, "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            if (existingByCode != null && existingByCode.getId() != null && !Objects.equals(existingByCode.getId(), editingProduct.getId())) {
                JOptionPane.showMessageDialog(this, "El código ya existe: " + code, "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        String name = nvl(nameField.getText());
        String category = nvl(categoryField.getText());
        String supplier = nvl(supplierField.getText());
        String location = nvl(locationField.getText());

        Double purchasePrice = parseRequiredPositiveDouble(purchasePriceField.getText(), "Precio compra");
        if (purchasePrice == null) return;

        Double salePrice = parseRequiredPositiveDouble(salePriceField.getText(), "Precio venta");
        if (salePrice == null) return;

        Integer minStockAlert = parseRequiredNonNegativeInt(minStockAlertField.getText(), "Stock mínimo");
        if (minStockAlert == null) return;

        Product toSave = new Product();
        if (editingProduct != null) {
            toSave.setId(editingProduct.getId());
            // Conserva stock actual en ediciones
            toSave.setCurrentStock(editingProduct.getCurrentStock());
            toSave.setCreatedAt(editingProduct.getCreatedAt());
        } else {
            toSave.setCurrentStock(0);
        }

        toSave.setCode(code);
        toSave.setName(name);
        toSave.setCategory(category);
        toSave.setSupplier(supplier);
        toSave.setLocation(location);
        toSave.setPurchasePrice(purchasePrice);
        toSave.setSalePrice(salePrice);
        toSave.setMinStockAlert(minStockAlert);
        toSave.setUpdatedAt(java.time.LocalDateTime.now());

        boolean ok;
        if (editingProduct == null) {
            Product created = productService.createProduct(toSave);
            ok = created != null;
            if (ok) {
                toSave = created;
                saved = true;
                logger.info("Product saved (created) code={}", code);
            }
        } else {
            ok = productService.updateProduct(toSave);
            if (ok) {
                saved = true;
                logger.info("Product saved (updated) id={} code={}", toSave.getId(), code);
            }
        }

        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el producto.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        dispose();
    }

    public boolean isSaved() {
        return saved;
    }

    private static void addRow(JPanel form, GridBagConstraints base, JLabel label, JTextField field, int row) {
        GridBagConstraints c = (GridBagConstraints) base.clone();
        c.gridy = row;

        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(label, c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }

    private Double parsePositiveDoubleOrNull(String text, String fieldLabel) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return null;

        try {
            double v = Double.parseDouble(t.replace(',', '.'));
            if (v <= 0) {
                JOptionPane.showMessageDialog(this, fieldLabel + " debe ser positivo (> 0).", "Validación", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido en " + fieldLabel + ".", "Validación", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private Integer parseNonNegativeIntOrNull(String text, String fieldLabel) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return null;

        try {
            int v = Integer.parseInt(t);
            if (v < 0) {
                JOptionPane.showMessageDialog(this, fieldLabel + " debe ser >= 0.", "Validación", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido en " + fieldLabel + ".", "Validación", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private Double parseRequiredPositiveDouble(String text, String fieldLabel) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            JOptionPane.showMessageDialog(this, fieldLabel + " es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return parsePositiveDoubleOrNull(t, fieldLabel);
    }

    private Integer parseRequiredNonNegativeInt(String text, String fieldLabel) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            JOptionPane.showMessageDialog(this, fieldLabel + " es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return parseNonNegativeIntOrNull(t, fieldLabel);
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }
}

