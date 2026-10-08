package com.library.ui;

import com.library.exception.LibraryException;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Small Swing helpers shared by all screens. */
final class Ui {
    private Ui() { }

    /** An action that may throw; errors are shown in a dialog instead of crashing the UI. */
    @FunctionalInterface
    interface Action { void run() throws Exception; }

    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(24);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }

    static JButton button(Component owner, String text, Action action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> {
            try {
                action.run();
            } catch (LibraryException ex) {
                error(owner, ex.getMessage());
            } catch (NumberFormatException ex) {
                error(owner, "Please enter valid numbers.");
            } catch (Exception ex) {
                error(owner, "Unexpected error: " + ex.getMessage());
            }
        });
        return b;
    }

    static JPanel row(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        for (Component c : comps) p.add(c);
        return p;
    }

    /** Selected row's first column (the ID), or an exception if nothing is selected. */
    static int selectedId(JTable t) throws LibraryException {
        int r = t.getSelectedRow();
        if (r < 0) throw new LibraryException("Please select a row first.");
        return (Integer) t.getValueAt(r, 0);
    }

    static void info(Component owner, String msg) {
        JOptionPane.showMessageDialog(owner, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    static void error(Component owner, String msg) {
        JOptionPane.showMessageDialog(owner, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static boolean confirm(Component owner, String msg) {
        return JOptionPane.showConfirmDialog(owner, msg, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
