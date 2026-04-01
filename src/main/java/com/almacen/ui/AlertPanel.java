package com.almacen.ui;

import com.almacen.model.Product;
import com.almacen.service.ProductService;
import com.almacen.service.NotificationService;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AlertPanel extends JPanel {
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(AlertPanel.class);
    
    private final ProductService productService;
    private final NotificationService notificationService;
    private final JPanel alertsContainer;
    private final JLabel statusLabel;
    private final JButton refreshButton;
    private final JButton generateOrderButton;
    
    private List<Product> previousAlerts = new ArrayList<>();
    private Timer refreshTimer;
    
    public AlertPanel() {
        this.productService = new ProductService();
        this.notificationService = new NotificationService();
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Contenedor de alertas
        alertsContainer = new JPanel();
        alertsContainer.setLayout(new BoxLayout(alertsContainer, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(alertsContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        
        // Inicializar timer de actualización
        startRefreshTimer();
        
        // Cargar alertas iniciales
        refreshAlerts();
    }
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        
        // Título
        JLabel titleLabel = new JLabel("Alertas de Stock Bajo");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(Color.DARK_GRAY);
        
        // Status label
        statusLabel = new JLabel("Cargando...");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(Color.GRAY);
        
        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("Refrescar");
        refreshButton.setToolTipText("Actualizar alertas (F5)");
        generateOrderButton = new JButton("Generar Pedido");
        generateOrderButton.setToolTipText("Crear pedido para productos con stock bajo");
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(generateOrderButton);
        
        // Panel de título y status
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(statusLabel, BorderLayout.SOUTH);
        
        header.add(titlePanel, BorderLayout.CENTER);
        header.add(buttonPanel, BorderLayout.EAST);
        
        // Event listeners
        refreshButton.addActionListener(e -> refreshAlerts());
        generateOrderButton.addActionListener(e -> generateOrder());
        
        // Setup shortcuts
        setupShortcuts();
        
        return header;
    }
    
    private void setupShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        
        // F5 para refrescar
        KeyStroke f5 = KeyStroke.getKeyStroke("F5");
        inputMap.put(f5, "refresh");
        actionMap.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                refreshAlerts();
            }
        });
    }
    
    private void startRefreshTimer() {
        refreshTimer = new Timer("AlertRefreshTimer", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(AlertPanel.this::refreshAlerts);
            }
        }, 5 * 60 * 1000, 5 * 60 * 1000); // Cada 5 minutos
    }
    
    private void refreshAlerts() {
        try {
            List<Product> lowStockProducts = productService.getLowStockProducts();
            
            // Detectar nuevas alertas
            List<Product> newAlerts = findNewAlerts(lowStockProducts);
            if (!newAlerts.isEmpty()) {
                showNewAlertsNotification(newAlerts);
            }
            
            // Actualizar UI
            updateAlertsDisplay(lowStockProducts);
            previousAlerts = new ArrayList<>(lowStockProducts);
            
            // Actualizar status
            SwingUtilities.invokeLater(() -> {
                int count = lowStockProducts.size();
                if (count == 0) {
                    statusLabel.setText("No hay alertas activas");
                    statusLabel.setForeground(Color.GREEN);
                } else {
                    statusLabel.setText(String.format("%d alerta%s activa%s", count, count == 1 ? "" : "s", count == 1 ? "" : "s"));
                    statusLabel.setForeground(Color.ORANGE);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error refreshing alerts", e);
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Error al cargar alertas");
                statusLabel.setForeground(Color.RED);
            });
        }
    }
    
    private List<Product> findNewAlerts(List<Product> currentAlerts) {
        List<Product> newAlerts = new ArrayList<>();
        for (Product current : currentAlerts) {
            boolean found = false;
            for (Product previous : previousAlerts) {
                if (current.getId().equals(previous.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                newAlerts.add(current);
            }
        }
        return newAlerts;
    }
    
    private void showNewAlertsNotification(List<Product> newAlerts) {
        String title = "Nuevas Alertas de Stock";
        String message = String.format("Hay %d producto%s con stock bajo:%n%s", 
            newAlerts.size(), 
            newAlerts.size() == 1 ? "" : "s",
            formatProductList(newAlerts));
        
        notificationService.showNotification(title, message);
    }
    
    private String formatProductList(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(products.size(), 3); i++) {
            Product p = products.get(i);
            sb.append("• ").append(p.getName())
              .append(" (Stock: ").append(p.getCurrentStock())
              .append(" / Mín: ").append(p.getMinStockAlert()).append(")");
            if (i < Math.min(products.size(), 3) - 1) {
                sb.append("\n");
            }
        }
        if (products.size() > 3) {
            sb.append("\n... y ").append(products.size() - 3).append(" más");
        }
        return sb.toString();
    }
    
    private void updateAlertsDisplay(List<Product> lowStockProducts) {
        SwingUtilities.invokeLater(() -> {
            alertsContainer.removeAll();
            
            if (lowStockProducts.isEmpty()) {
                JPanel emptyPanel = new JPanel(new FlowLayout());
                emptyPanel.add(new JLabel("No hay alertas de stock bajo"));
                alertsContainer.add(emptyPanel);
            } else {
                for (Product product : lowStockProducts) {
                    alertsContainer.add(createAlertCard(product));
                    alertsContainer.add(Box.createVerticalStrut(10));
                }
            }
            
            alertsContainer.revalidate();
            alertsContainer.repaint();
        });
    }
    
    private JPanel createAlertCard(Product product) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.ORANGE, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setBackground(new Color(255, 253, 235));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredHeight()));
        
        // Información del producto
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(product.getName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setForeground(Color.DARK_GRAY);
        
        JLabel stockLabel = new JLabel(String.format("Stock actual: %d | Mínimo requerido: %d", 
            product.getCurrentStock(), product.getMinStockAlert()));
        stockLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stockLabel.setForeground(Color.RED);
        
        JLabel messageLabel = new JLabel(getAlertMessage(product));
        messageLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        messageLabel.setForeground(Color.GRAY);
        
        infoPanel.add(nameLabel);
        infoPanel.add(stockLabel);
        infoPanel.add(messageLabel);
        
        // Botones de acción
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        
        JButton resolveButton = new JButton("Resolver");
        resolveButton.setPreferredSize(new Dimension(80, 25));
        resolveButton.setBackground(new Color(76, 175, 80));
        resolveButton.setForeground(Color.WHITE);
        resolveButton.setFocusPainted(false);
        
        JButton ignoreButton = new JButton("Ignorar");
        ignoreButton.setPreferredSize(new Dimension(80, 25));
        ignoreButton.setBackground(new Color(158, 158, 158));
        ignoreButton.setForeground(Color.WHITE);
        ignoreButton.setFocusPainted(false);
        
        JButton orderButton = new JButton("Pedido");
        orderButton.setPreferredSize(new Dimension(80, 25));
        orderButton.setBackground(new Color(33, 150, 243));
        orderButton.setForeground(Color.WHITE);
        orderButton.setFocusPainted(false);
        
        buttonPanel.add(resolveButton);
        buttonPanel.add(ignoreButton);
        buttonPanel.add(orderButton);
        
        // Event listeners
        resolveButton.addActionListener(e -> resolveAlert(product, card));
        ignoreButton.addActionListener(e -> ignoreAlert(product, card));
        orderButton.addActionListener(e -> openOrderDialog(product));
        
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);
        
        return card;
    }
    
    private String getAlertMessage(Product product) {
        int current = product.getCurrentStock();
        int min = product.getMinStockAlert();
        int deficit = min - current;
        
        if (current == 0) {
            return "⚠️ AGOTADO - Necesita reabastecimiento urgente";
        } else if (deficit <= min * 0.5) {
            return "⚠️ Stock crítico - Requiere atención inmediata";
        } else {
            return "📉 Stock bajo - Considerar reabastecimiento";
        }
    }
    
    private void resolveAlert(Product product, JPanel card) {
        // Aquí podrías agregar lógica para registrar que la alerta fue resuelta
        // Por ahora, simplemente removemos la tarjeta
        alertsContainer.remove(card);
        alertsContainer.revalidate();
        alertsContainer.repaint();
        
        notificationService.showNotification("Alerta Resuelta", 
            "Se ha marcado como resuelta la alerta de: " + product.getName());
    }
    
    private void ignoreAlert(Product product, JPanel card) {
        // Similar a resolve, pero con diferente notificación
        alertsContainer.remove(card);
        alertsContainer.revalidate();
        alertsContainer.repaint();
        
        notificationService.showNotification("Alerta Ignorada", 
            "Se ha ignorado la alerta de: " + product.getName());
    }
    
    private void openOrderDialog(Product product) {
        StringBuilder orderInfo = new StringBuilder();
        orderInfo.append("=== DATOS DE PEDIDO ===\n\n");
        orderInfo.append("Producto: ").append(product.getName()).append("\n");
        orderInfo.append("Código: ").append(product.getCode()).append("\n");
        orderInfo.append("Stock actual: ").append(product.getCurrentStock()).append("\n");
        orderInfo.append("Stock mínimo: ").append(product.getMinStockAlert()).append("\n");
        orderInfo.append("Sugerencia de pedido: ").append(product.getMinStockAlert() * 2).append("\n\n");
        
        if (product.getSupplier() != null && !product.getSupplier().trim().isEmpty()) {
            orderInfo.append("Proveedor: ").append(product.getSupplier()).append("\n");
        }
        
        if (product.getLocation() != null && !product.getLocation().trim().isEmpty()) {
            orderInfo.append("Ubicación: ").append(product.getLocation()).append("\n");
        }
        
        orderInfo.append("\nFecha: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        
        JTextArea textArea = new JTextArea(orderInfo.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        
        JOptionPane.showMessageDialog(this, scrollPane, 
            "Generar Pedido - " + product.getName(), JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void generateOrder() {
        List<Product> lowStockProducts = productService.getLowStockProducts();
        if (lowStockProducts.isEmpty()) {
            notificationService.showNotification("Sin Alertas", 
                "No hay productos con stock bajo para generar pedido");
            return;
        }
        
        StringBuilder orderInfo = new StringBuilder();
        orderInfo.append("=== PEDIDO MASIVO ===\n\n");
        orderInfo.append("Productos con stock bajo:\n\n");
        
        for (Product product : lowStockProducts) {
            orderInfo.append("• ").append(product.getName()).append("\n");
            orderInfo.append("  Código: ").append(product.getCode()).append("\n");
            orderInfo.append("  Stock actual: ").append(product.getCurrentStock()).append("\n");
            orderInfo.append("  Sugerencia: ").append(product.getMinStockAlert() * 2).append("\n");
            if (product.getSupplier() != null && !product.getSupplier().trim().isEmpty()) {
                orderInfo.append("  Proveedor: ").append(product.getSupplier()).append("\n");
            }
            orderInfo.append("\n");
        }
        
        orderInfo.append("Fecha: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        
        JTextArea textArea = new JTextArea(orderInfo.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(this, scrollPane, 
            "Generar Pedido Masivo", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private int getPreferredHeight() {
        return 120;
    }
    
    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }
}
