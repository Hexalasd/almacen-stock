package com.almacen;

import com.almacen.ui.LoginFrame;
import com.almacen.utils.DatabaseInitializer;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        setNimbusLookAndFeel();
        DatabaseInitializer.initializeIfNeeded();

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }

    private static void setNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Fallback a LAF por defecto
        }
    }
}
