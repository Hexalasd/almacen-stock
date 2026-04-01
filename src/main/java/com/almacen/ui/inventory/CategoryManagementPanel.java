package com.almacen.ui.inventory;

import com.almacen.model.Category;
import com.almacen.service.CategoryService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class CategoryManagementPanel extends JPanel {
    private final CategoryService categoryService;
    private final CategoryTableModel tableModel;
    private final JTable table;

    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;

    public CategoryManagementPanel() {
        this.categoryService = new CategoryService();
        this.tableModel = new CategoryTableModel();
        this.table = new JTable(tableModel);
        this.table.setFillsViewportHeight(true);
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.table.setRowHeight(24);

        // Doble click para editar
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

        addButton = new JButton("Agregar");
        editButton = new JButton("Editar");
        deleteButton = new JButton("Eliminar");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(deleteButton);
        buttons.add(editButton);
        buttons.add(addButton);

        setLayout(new BorderLayout(10, 10));
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addCategory());
        editButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) editSelected(row);
        });
        deleteButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) deleteSelected(row);
        });

        refreshCategories();
    }

    private void refreshCategories() {
        List<Category> categories = categoryService.getAllCategories();
        tableModel.setCategories(categories);
    }

    private void addCategory() {
        String name = JOptionPane.showInputDialog(
            this,
            "Nombre de la nueva categoría:",
            "Agregar Categoría",
            JOptionPane.QUESTION_MESSAGE
        );

        if (name != null && !name.trim().isEmpty()) {
            try {
                categoryService.createCategory(name.trim());
                refreshCategories();
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editSelected(int row) {
        Category category = tableModel.getCategoryAt(row);
        if (category == null) return;

        String newName = JOptionPane.showInputDialog(
            this,
            "Nuevo nombre de la categoría:",
            "Editar Categoría",
            JOptionPane.QUESTION_MESSAGE
        );

        if (newName != null && !newName.trim().isEmpty()) {
            try {
                category.setName(newName.trim());
                categoryService.updateCategory(category);
                refreshCategories();
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelected(int row) {
        Category category = tableModel.getCategoryAt(row);
        if (category == null || category.getId() == null) return;

        int option = JOptionPane.showConfirmDialog(
            this,
            "¿Eliminar la categoría '" + category.getName() + "'?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {
            try {
                categoryService.deleteCategory(category.getId());
                refreshCategories();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "No se pudo eliminar la categoría. Es posible que tenga productos asociados.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class CategoryTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Nombre", "Fecha Creación"};
        private List<Category> categories = new ArrayList<>();

        @Override
        public int getRowCount() {
            return categories.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        public Category getCategoryAt(int row) {
            if (row < 0 || row >= categories.size()) return null;
            return categories.get(row);
        }

        public void setCategories(List<Category> newCategories) {
            this.categories = newCategories == null ? new ArrayList<>() : newCategories;
            fireTableDataChanged();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Category category = getCategoryAt(rowIndex);
            if (category == null) return null;

            return switch (columnIndex) {
                case 0 -> category.getId();
                case 1 -> category.getName();
                case 2 -> category.getCreatedAt() != null ? 
                    category.getCreatedAt().toLocalDate().toString() : "-";
                default -> null;
            };
        }
    }
}
