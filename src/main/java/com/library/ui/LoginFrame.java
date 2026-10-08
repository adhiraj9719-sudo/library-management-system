package com.library.ui;

import com.library.dao.MemberDAO;
import com.library.exception.LibraryException;
import com.library.model.Librarian;
import com.library.model.Member;
import com.library.model.Person;
import com.library.util.PasswordUtil;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginFrame extends JFrame {
    private final MemberDAO memberDAO = new MemberDAO();
    private final JTextField email = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);

    public LoginFrame() {
        super("Library Management System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Email:"));    form.add(email);
        form.add(new JLabel("Password:")); form.add(password);
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

        JPanel buttons = Ui.row(
            Ui.button(this, "Login", this::doLogin),
            Ui.button(this, "Register as Member", this::doRegister));

        add(form, java.awt.BorderLayout.CENTER);
        add(buttons, java.awt.BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() throws LibraryException {
        Person p = memberDAO.authenticate(email.getText().trim(), new String(password.getPassword()));
        // Polymorphism: both subclasses are handled through the Person reference
        if (p instanceof Librarian l) new LibrarianDashboard(l).setVisible(true);
        else new MemberDashboard((Member) p).setVisible(true);
        dispose();
    }

    private void doRegister() throws LibraryException {
        JTextField n = new JTextField(), e = new JTextField();
        JPasswordField pw = new JPasswordField();
        Object[] fields = {"Name:", n, "Email:", e, "Password:", pw};
        if (JOptionPane.showConfirmDialog(this, fields, "Register", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String raw = new String(pw.getPassword());
        if (n.getText().isBlank() || e.getText().isBlank() || raw.length() < 4)
            throw new LibraryException("Name, email and a password of at least 4 characters are required.");
        memberDAO.add(new Member(0, n.getText().trim(), e.getText().trim(), PasswordUtil.hash(raw), true));
        Ui.info(this, "Account created. You can log in now.");
    }
}
