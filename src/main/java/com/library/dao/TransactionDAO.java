package com.library.dao;

import com.library.exception.BookNotAvailableException;
import com.library.exception.LibraryException;
import com.library.model.LoanRecord;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransactionDAO {
    public static final int LOAN_DAYS = 14;

    private static final String SELECT =
        "SELECT t.id, t.book_id, t.member_id, b.title, u.name, t.borrow_date, t.due_date, t.return_date "
      + "FROM transactions t JOIN books b ON b.id=t.book_id JOIN users u ON u.id=t.member_id ";

    private static LoanRecord map(ResultSet rs) throws SQLException {
        Date ret = rs.getDate(8);
        return new LoanRecord(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getString(5),
                rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), ret == null ? null : ret.toLocalDate());
    }

    public List<LoanRecord> findAll() throws LibraryException {
        return DBConnection.query(SELECT + "ORDER BY t.id DESC", TransactionDAO::map);
    }

    public List<LoanRecord> findByMember(int memberId) throws LibraryException {
        return DBConnection.query(SELECT + "WHERE t.member_id=? ORDER BY t.id DESC", TransactionDAO::map, memberId);
    }

    /** Open loans due within the next {@code days} days (or already overdue). */
    public List<LoanRecord> findDueSoon(int days) throws LibraryException {
        return DBConnection.query(SELECT
                + "WHERE t.return_date IS NULL AND t.due_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY)",
                TransactionDAO::map, days);
    }

    /** Book title -> number of times borrowed (for the trends chart). */
    public Map<String, Integer> topBorrowed(int limit) throws LibraryException {
        Map<String, Integer> out = new LinkedHashMap<>();
        DBConnection.query("SELECT b.title, COUNT(*) c FROM transactions t JOIN books b ON b.id=t.book_id "
                + "GROUP BY b.id, b.title ORDER BY c DESC LIMIT ?",
                rs -> out.put(rs.getString(1), rs.getInt(2)), limit);
        return out;
    }

    public void borrow(int bookId, int memberId, int maxActive) throws LibraryException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);                       // one atomic DB transaction
            try {
                int avail = scalar(c, "SELECT available_copies FROM books WHERE id=? FOR UPDATE",
                        "Book ID " + bookId + " not found", bookId);
                scalar(c, "SELECT id FROM users WHERE id=? AND role='MEMBER'",
                        "Member ID " + memberId + " not found", memberId);
                if (avail <= 0) throw new BookNotAvailableException("No copies of this book are currently available.");
                int active = scalar(c, "SELECT COUNT(*) FROM transactions WHERE member_id=? AND return_date IS NULL",
                        "", memberId);
                if (active >= maxActive) throw new LibraryException("Borrow limit reached (" + maxActive + " books).");

                run(c, "INSERT INTO transactions(book_id,member_id,borrow_date,due_date) VALUES(?,?,?,?)",
                        bookId, memberId, Date.valueOf(LocalDate.now()), Date.valueOf(LocalDate.now().plusDays(LOAN_DAYS)));
                run(c, "UPDATE books SET available_copies = available_copies - 1 WHERE id=?", bookId);
                c.commit();
            } catch (LibraryException e) {
                c.rollback();
                throw e;
            } catch (SQLException e) {
                c.rollback();
                throw new LibraryException("Borrow failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error: " + e.getMessage(), e);
        }
    }

    public void giveBack(int bookId, int memberId) throws LibraryException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                int txId = scalar(c, "SELECT id FROM transactions WHERE book_id=? AND member_id=? "
                        + "AND return_date IS NULL LIMIT 1 FOR UPDATE",
                        "No active loan found for this book and member.", bookId, memberId);
                run(c, "UPDATE transactions SET return_date=? WHERE id=?", Date.valueOf(LocalDate.now()), txId);
                run(c, "UPDATE books SET available_copies = available_copies + 1 WHERE id=?", bookId);
                c.commit();
            } catch (LibraryException e) {
                c.rollback();
                throw e;
            } catch (SQLException e) {
                c.rollback();
                throw new LibraryException("Return failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error: " + e.getMessage(), e);
        }
    }

    private static int scalar(Connection c, String sql, String notFound, Object... args)
            throws SQLException, LibraryException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            DBConnection.bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new LibraryException(notFound);
                return rs.getInt(1);
            }
        }
    }

    private static void run(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            DBConnection.bind(ps, args);
            ps.executeUpdate();
        }
    }
}
