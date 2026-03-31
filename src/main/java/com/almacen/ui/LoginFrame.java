package com.almacen.ui;

import com.almacen.model.User;
import com.almacen.security.AuthService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private static final Logger logger = LogManager.getLogger(LoginFrame.class);

    private final AuthService authService;

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JButton exitButton;

    public LoginFrame() {
        super("Sistema de Gestión de Stock - Login");
        this.authService = new AuthService();

        JLabel title = new JLabel("Sistema de Gestión de Stock", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JLabel userLabel = new JLabel("Usuario:");
        JLabel passLabel = new JLabel("Contraseña:");

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);

        loginButton = new JButton("Iniciar Sesión");
        exitButton = new JButton("Salir");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(title, c);

        c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        c.gridx = 0; c.gridy = 1;
        form.add(userLabel, c);
        c.gridx = 1; c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(usernameField, c);

        c.gridx = 0; c.gridy = 2;
        c.fill = GridBagConstraints.NONE;
        form.add(passLabel, c);
        c.gridx = 1; c.gridy = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(passwordField, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(exitButton);
        buttons.add(loginButton);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(loginButton); // Enter para enviar

        loginButton.addActionListener(e -> doLogin());
        exitButton.addActionListener(e -> System.exit(0));

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(this, "Ingrese usuario y contraseña.", "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = authService.login(username, password);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.", "Login", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                passwordField.requestFocusInWindow();
                return;
            }

            SwingUtilities.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame(authService);
                mainFrame.setVisible(true);
                dispose();
            });
        } catch (Exception ex) {
            logger.error("Unexpected error during login", ex);
            JOptionPane.showMessageDialog(this, "Ocurrió un error al iniciar sesión.", "Login", JOptionPane.ERROR_MESSAGE);
        }
    }
}

