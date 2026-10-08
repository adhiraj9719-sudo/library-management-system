package com.library;

import com.library.service.DueDateNotifier;
import com.library.ui.LoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        new DueDateNotifier().start(60);                       // hourly background reminders
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));   // Swing runs on the EDT
    }
}
