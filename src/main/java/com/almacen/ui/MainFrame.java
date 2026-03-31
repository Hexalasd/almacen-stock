package com.almacen.ui;

import com.almacen.model.User;
import com.almacen.security.AuthService;
import com.almacen.ui.inventory.ProductPanel;
import com.almacen.ui.inventory.StockPanel;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainFrame extends JFrame {
    private final AuthService authService;

    private final JLabel statusLabel;

    public MainFrame(AuthService authService) {
        super("Sistema de Gestión de Stock");
        if (authService == null) {
            throw new IllegalArgumentException("authService cannot be null");
        }
        this.authService = authService;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Bienvenido", createWelcomePanel());
        tabs.addTab("Inventario", new ProductPanel());
        tabs.addTab("Stock", new StockPanel(authService));

        setJMenuBar(createMenuBar(tabs));

        statusLabel = new JLabel(buildStatusText());
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusBar.add(statusLabel, BorderLayout.WEST);

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmExit();
            }
        });

        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar(JTabbedPane tabs) {
        JMenuBar bar = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> confirmExit());
        archivo.add(salir);

        JMenu inventario = new JMenu("Inventario");
        JMenuItem productos = new JMenuItem("Productos");
        productos.addActionListener(e -> tabs.setSelectedIndex(1));
        inventario.add(productos);
        JMenuItem stock = new JMenuItem("Stock");
        stock.addActionListener(e -> tabs.setSelectedIndex(2));
        inventario.add(stock);

        JMenu reportes = new JMenu("Reportes");
        JMenu ayuda = new JMenu("Ayuda");

        bar.add(archivo);
        bar.add(inventario);
        bar.add(reportes);
        bar.add(ayuda);

        return bar;
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel welcome = new JLabel("Bienvenido");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 22f));
        panel.add(welcome);
        return panel;
    }

    private void confirmExit() {
        int option = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas salir de la aplicación?",
                "Confirmar salida",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (option == JOptionPane.YES_OPTION) {
            authService.logout();
            dispose();
            System.exit(0);
        }
    }

    private String buildStatusText() {
        User user = authService.getCurrentUser();
        String userText = (user == null || user.getUsername() == null) ? "Usuario: -" : "Usuario: " + user.getUsername();
        String dateText = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return userText + "   |   " + dateText;
    }
}

