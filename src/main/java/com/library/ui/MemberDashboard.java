package com.library.ui;

import com.library.dao.BookDAO;
import com.library.dao.MemberDAO;
import com.library.dao.NotificationDAO;
import com.library.dao.TransactionDAO;
import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.LoanRecord;
import com.library.model.Member;
import com.library.model.Notification;
import com.library.service.LibraryService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class MemberDashboard extends JFrame {
    private static final int NOTIF_TAB = 4;

    private final Member me;
    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final LibraryService service = new LibraryService();

    private final DefaultTableModel resultModel = Ui.model("ID", "Title", "Author", "Genre", "Available");
    private final DefaultTableModel historyModel =
            Ui.model("ID", "Book ID", "Book", "Borrowed", "Due", "Returned");
    private final DefaultTableModel searchLogModel = Ui.model("Search", "Time");
    private final DefaultListModel<String> alertModel = new DefaultListModel<>();
    private final JTabbedPane tabs = new JTabbedPane();
    private ScheduledExecutorService alertPoller;

    public MemberDashboard(Member me) {
        super("Member Dashboard - " + me.getName());
        this.me = me;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(850, 560);
        setLocationRelativeTo(null);

        tabs.addTab("Search & Borrow", searchTab());
        tabs.addTab("Borrowing History", historyTab());
        tabs.addTab("Search History", searchLogTab());
        tabs.addTab("Profile", profileTab());
        tabs.addTab("Notifications", notificationsTab());
        add(tabs);

        refreshHistory();
        refreshSearchLog();
        startAlertPoller();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { alertPoller.shutdownNow(); }
        });
    }

    // ------------------------------------------------------------ Search tab
    private JPanel searchTab() {
        JComboBox<String> field = new JComboBox<>(new String[]{"Title", "Author", "Genre"});
        JTextField keyword = new JTextField(20);
        JTable t = Ui.table(resultModel);

        Ui.Action search = () -> {
            resultModel.setRowCount(0);
            for (Book b : bookDAO.search((String) field.getSelectedItem(), keyword.getText().trim()))
                resultModel.addRow(new Object[]{b.id(), b.title(), b.author(), b.genre(), b.availableCopies()});
            bookDAO.logSearch(me.getId(), field.getSelectedItem() + ": " + keyword.getText().trim());
            refreshSearchLog();
        };

        JPanel p = new JPanel(new BorderLayout());
        p.add(Ui.row(new JLabel("Search by"), field, keyword, Ui.button(this, "Search", search)), BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        p.add(Ui.row(Ui.button(this, "Borrow Selected Book", () -> {
            service.borrowBook(Ui.selectedId(t), me.getId());
            Ui.info(this, "Book borrowed successfully. Due in " + TransactionDAO.LOAN_DAYS + " days.");
            search.run();
            refreshHistory();
        })), BorderLayout.SOUTH);
        return p;
    }

    // ----------------------------------------------------------- History tab
    private JPanel historyTab() {
        JTable t = Ui.table(historyModel);
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        p.add(Ui.row(Ui.button(this, "Return Selected Book", () -> {
            int r = t.getSelectedRow();
            if (r < 0) throw new LibraryException("Select a borrowed book first.");
            if (!t.getValueAt(r, 5).toString().isEmpty()) throw new LibraryException("This book was already returned.");
            service.returnBook((Integer) t.getValueAt(r, 1), me.getId());
            Ui.info(this, "Book returned successfully.");
            refreshHistory();
        }), Ui.button(this, "Refresh", this::refreshHistory)), BorderLayout.SOUTH);
        return p;
    }

    private void refreshHistory() {
        try {
            historyModel.setRowCount(0);
            for (LoanRecord l : transactionDAO.findByMember(me.getId()))
                historyModel.addRow(new Object[]{l.id(), l.bookId(), l.bookTitle(), l.borrowDate(), l.dueDate(),
                        l.returnDate() == null ? "" : l.returnDate()});
        } catch (LibraryException e) {
            Ui.error(this, e.getMessage());
        }
    }

    private JPanel searchLogTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(Ui.table(searchLogModel)), BorderLayout.CENTER);
        return p;
    }

    private void refreshSearchLog() {
        try {
            searchLogModel.setRowCount(0);
            for (String[] row : bookDAO.searchHistory(me.getId())) searchLogModel.addRow(row);
        } catch (LibraryException e) {
            Ui.error(this, e.getMessage());
        }
    }

    // ----------------------------------------------------------- Profile tab
    private JPanel profileTab() {
        JTextField name = new JTextField(me.getName(), 20);
        JTextField email = new JTextField(me.getEmail(), 20);
        JPasswordField pw = new JPasswordField(20);
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Name:"));                          form.add(name);
        form.add(new JLabel("Email:"));                         form.add(email);
        form.add(new JLabel("New password (blank = keep):"));   form.add(pw);
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel p = new JPanel(new BorderLayout());
        p.add(form, BorderLayout.NORTH);
        p.add(Ui.row(Ui.button(this, "Save Profile", () -> {
            if (name.getText().isBlank() || email.getText().isBlank())
                throw new LibraryException("Name and email cannot be empty.");
            me.setName(name.getText().trim());
            me.setEmail(email.getText().trim());
            memberDAO.update(me);
            String raw = new String(pw.getPassword());
            if (!raw.isEmpty()) {
                if (raw.length() < 4) throw new LibraryException("Password must be at least 4 characters.");
                memberDAO.updatePassword(me.getId(), raw);
                pw.setText("");
            }
            Ui.info(this, "Profile updated successfully.");
        })), BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------ Notifications tab
    private JPanel notificationsTab() {
        JCheckBox enabled = new JCheckBox("Receive due-date and new-arrival notifications", me.isNotifyEnabled());
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(new JList<>(alertModel)), BorderLayout.CENTER);
        p.add(Ui.row(enabled,
            Ui.button(this, "Save Preference", () -> {
                me.setNotifyEnabled(enabled.isSelected());
                memberDAO.update(me);
                Ui.info(this, "Notification settings saved successfully.");
            }),
            Ui.button(this, "Mark All Read", () -> { notificationDAO.markAllRead(me.getId()); pollAlerts(); })),
            BorderLayout.SOUTH);
        return p;
    }

    /**
     * Background thread: every 10 s asks the DB for notifications and updates the UI.
     * Swing components must only be touched on the Event Dispatch Thread, hence invokeLater.
     */
    private void startAlertPoller() {
        alertPoller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alert-poller-" + me.getId());
            t.setDaemon(true);
            return t;
        });
        alertPoller.scheduleWithFixedDelay(this::pollAlerts, 0, 10, TimeUnit.SECONDS);
    }

    private void pollAlerts() {
        try {
            int unread = notificationDAO.unreadCount(me.getId());
            java.util.List<Notification> all = notificationDAO.findByMember(me.getId());
            SwingUtilities.invokeLater(() -> {
                alertModel.clear();
                for (Notification n : all)
                    alertModel.addElement((n.read() ? "   " : "● ") + n.createdAt().toString().replace('T', ' ') + "  -  " + n.message());
                tabs.setTitleAt(NOTIF_TAB, unread > 0 ? "Notifications (" + unread + ")" : "Notifications");
            });
        } catch (LibraryException e) {
            System.err.println("[alert-poller] " + e.getMessage());
        }
    }
}
