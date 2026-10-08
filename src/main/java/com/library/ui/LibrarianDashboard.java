package com.library.ui;

import com.library.dao.BookDAO;
import com.library.dao.MemberDAO;
import com.library.dao.NotificationDAO;
import com.library.dao.TransactionDAO;
import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.Librarian;
import com.library.model.LoanRecord;
import com.library.model.Member;
import com.library.service.DueDateNotifier;
import com.library.service.LibraryService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

public class LibrarianDashboard extends JFrame {
    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final LibraryService service = new LibraryService();
    private final DueDateNotifier notifier = new DueDateNotifier();

    private final DefaultTableModel bookModel = Ui.model("ID", "Title", "Author", "Genre", "Total", "Available");
    private final DefaultTableModel memberModel = Ui.model("ID", "Name", "Email", "Notifications");
    private final DefaultTableModel txModel =
            Ui.model("ID", "Book ID", "Book", "Member ID", "Member", "Borrowed", "Due", "Returned");
    private final JComboBox<Object> recipientBox = new JComboBox<>();
    private final JTextArea reportArea = new JTextArea(10, 40);
    private final BarChartPanel chart = new BarChartPanel("Most borrowed books");

    public LibrarianDashboard(Librarian me) {
        super("Librarian Dashboard - " + me.getName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Books", booksTab());
        tabs.addTab("Members", membersTab());
        tabs.addTab("Transactions", transactionsTab());
        tabs.addTab("Notifications", notificationsTab());
        tabs.addTab("Inventory Reports", reportsTab());
        tabs.addChangeListener(e -> refreshAll());
        add(tabs);
        refreshAll();
    }

    // ---------------------------------------------------------------- Books
    private JPanel booksTab() {
        JTable t = Ui.table(bookModel);
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        p.add(Ui.row(
            Ui.button(this, "Add Book", () -> {
                Book b = bookForm(null);
                if (b != null) { bookDAO.add(b); refreshAll(); Ui.info(this, "Book added successfully."); }
            }),
            Ui.button(this, "Edit Book", () -> {
                Book b = bookForm(bookDAO.findById(Ui.selectedId(t)));
                if (b != null) { bookDAO.update(b); refreshAll(); Ui.info(this, "Book updated successfully."); }
            }),
            Ui.button(this, "Delete Book", () -> {
                int id = Ui.selectedId(t);
                if (Ui.confirm(this, "Delete book #" + id + "?")) { bookDAO.delete(id); refreshAll(); Ui.info(this, "Book deleted successfully."); }
            })), BorderLayout.SOUTH);
        return p;
    }

    private Book bookForm(Book b) throws LibraryException {
        JTextField title = new JTextField(b == null ? "" : b.title());
        JTextField author = new JTextField(b == null ? "" : b.author());
        JTextField genre = new JTextField(b == null ? "" : b.genre());
        JSpinner copies = new JSpinner(new SpinnerNumberModel(b == null ? 1 : b.totalCopies(), 0, 1000, 1));
        Object[] f = {"Title:", title, "Author:", author, "Genre:", genre, "Total copies:", copies};
        if (JOptionPane.showConfirmDialog(this, f, b == null ? "Add Book" : "Edit Book", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return null;
        if (title.getText().isBlank() || author.getText().isBlank() || genre.getText().isBlank())
            throw new LibraryException("Title, author and genre are required.");
        int total = (Integer) copies.getValue();
        return new Book(b == null ? 0 : b.id(), title.getText().trim(), author.getText().trim(),
                genre.getText().trim(), total, b == null ? total : b.availableCopies());
    }

    // -------------------------------------------------------------- Members
    private JPanel membersTab() {
        JTable t = Ui.table(memberModel);
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        p.add(Ui.row(
            Ui.button(this, "Create Member", () -> {
                Member m = memberForm(null);
                if (m != null) { memberDAO.add(m); refreshAll(); Ui.info(this, "Member created successfully."); }
            }),
            Ui.button(this, "Update Member", () -> {
                Member m = memberForm(memberDAO.findById(Ui.selectedId(t)));
                if (m != null) { memberDAO.update(m); refreshAll(); Ui.info(this, "Member updated successfully."); }
            }),
            Ui.button(this, "Delete Member", () -> {
                int id = Ui.selectedId(t);
                if (Ui.confirm(this, "Delete member #" + id + " and their history?")) {
                    memberDAO.delete(id); refreshAll(); Ui.info(this, "Member deleted successfully.");
                }
            })), BorderLayout.SOUTH);
        return p;
    }

    /** Returns the new/edited member (password applied on update if provided), or null if cancelled. */
    private Member memberForm(Member m) throws LibraryException {
        JTextField name = new JTextField(m == null ? "" : m.getName());
        JTextField email = new JTextField(m == null ? "" : m.getEmail());
        JPasswordField pw = new JPasswordField();
        Object[] f = {"Name:", name, "Email:", email, m == null ? "Password:" : "New password (blank = keep):", pw};
        if (JOptionPane.showConfirmDialog(this, f, m == null ? "Create Member" : "Update Member", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return null;
        String raw = new String(pw.getPassword());
        if (name.getText().isBlank() || email.getText().isBlank()) throw new LibraryException("Name and email are required.");
        if (m == null) {
            if (raw.length() < 4) throw new LibraryException("Password must be at least 4 characters.");
            return new Member(0, name.getText().trim(), email.getText().trim(), com.library.util.PasswordUtil.hash(raw), true);
        }
        m.setName(name.getText().trim());
        m.setEmail(email.getText().trim());
        if (!raw.isEmpty()) memberDAO.updatePassword(m.getId(), raw);
        return m;
    }

    // --------------------------------------------------------- Transactions
    private JPanel transactionsTab() {
        JTable t = Ui.table(txModel);
        JTextField bookId = new JTextField(5), memberId = new JTextField(5);
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        p.add(Ui.row(new JLabel("Book ID:"), bookId, new JLabel("Member ID:"), memberId,
            Ui.button(this, "Issue Book", () -> {
                service.borrowBook(Integer.parseInt(bookId.getText().trim()), Integer.parseInt(memberId.getText().trim()));
                refreshAll(); Ui.info(this, "Book issued successfully (due in " + TransactionDAO.LOAN_DAYS + " days).");
            }),
            Ui.button(this, "Return Selected", () -> {
                int r = t.getSelectedRow();
                if (r < 0) throw new LibraryException("Select a transaction first.");
                if (t.getValueAt(r, 7) != null && !t.getValueAt(r, 7).toString().isEmpty())
                    throw new LibraryException("This book was already returned.");
                service.returnBook((Integer) t.getValueAt(r, 1), (Integer) t.getValueAt(r, 3));
                refreshAll(); Ui.info(this, "Book returned successfully.");
            })), BorderLayout.SOUTH);
        return p;
    }

    // --------------------------------------------------------- Notifications
    private JPanel notificationsTab() {
        JTextArea msg = new JTextArea(5, 40);
        msg.setLineWrap(true);
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(Ui.row(new JLabel("Recipient:"), recipientBox), BorderLayout.NORTH);
        p.add(new JScrollPane(msg), BorderLayout.CENTER);
        p.add(Ui.row(
            Ui.button(this, "Send Notification", () -> {
                if (msg.getText().isBlank()) throw new LibraryException("Write a message first.");
                Object sel = recipientBox.getSelectedItem();
                int sent = (sel instanceof Member m)
                        ? notificationDAO.sendTo(List.of(m.getId()), msg.getText().trim())
                        : notificationDAO.broadcast(msg.getText().trim());
                msg.setText("");
                Ui.info(this, "Notification sent to " + sent + " member(s).");
            }),
            Ui.button(this, "Run Due-Date Check Now", () ->
                Ui.info(this, notifier.runOnce() + " due-date reminder(s) created."))), BorderLayout.SOUTH);
        return p;
    }

    // --------------------------------------------------------------- Reports
    private JPanel reportsTab() {
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(reportArea), BorderLayout.NORTH);
        p.add(chart, BorderLayout.CENTER);
        p.add(Ui.row(Ui.button(this, "Generate Report", this::refreshReports)), BorderLayout.SOUTH);
        return p;
    }

    private void refreshReports() throws LibraryException {
        List<Book> books = bookDAO.findAll();
        List<LoanRecord> loans = transactionDAO.findAll();
        int copies = books.stream().mapToInt(Book::totalCopies).sum();
        int available = books.stream().mapToInt(Book::availableCopies).sum();
        long active = loans.stream().filter(l -> !l.isReturned()).count();
        long overdue = loans.stream().filter(LoanRecord::isOverdue).count();
        Map<String, Long> byGenre = books.stream()
                .collect(Collectors.groupingBy(Book::genre, TreeMap::new, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Titles: %d | Copies: %d | Available: %d | On loan: %d%n", books.size(), copies, available, active));
        sb.append(String.format("Active loans: %d | Overdue: %d | Total transactions: %d%n", active, overdue, loans.size()));
        sb.append("Titles per genre: ").append(byGenre).append(System.lineSeparator());
        sb.append("Out of stock: ").append(books.stream().filter(b -> b.availableCopies() == 0).map(Book::title).toList());
        reportArea.setText(sb.toString());
        chart.setData(transactionDAO.topBorrowed(6));
    }

    // --------------------------------------------------------------- Refresh
    private void refreshAll() {
        try {
            bookModel.setRowCount(0);
            for (Book b : bookDAO.findAll())
                bookModel.addRow(new Object[]{b.id(), b.title(), b.author(), b.genre(), b.totalCopies(), b.availableCopies()});

            memberModel.setRowCount(0);
            recipientBox.removeAllItems();
            recipientBox.addItem("All members");
            for (Member m : memberDAO.findAll()) {
                memberModel.addRow(new Object[]{m.getId(), m.getName(), m.getEmail(), m.isNotifyEnabled() ? "On" : "Off"});
                recipientBox.addItem(m);
            }

            txModel.setRowCount(0);
            for (LoanRecord l : transactionDAO.findAll())
                txModel.addRow(new Object[]{l.id(), l.bookId(), l.bookTitle(), l.memberId(), l.memberName(),
                        l.borrowDate(), l.dueDate(), l.returnDate() == null ? "" : l.returnDate()});
            refreshReports();
        } catch (LibraryException e) {
            Ui.error(this, e.getMessage());
        }
    }
}
