package com.library.dao;

import com.library.exception.LibraryException;
import com.library.model.Book;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class BookDAO implements GenericDAO<Book>, Searchable<Book> {

    private static Book map(ResultSet rs) throws SQLException {
        return new Book(rs.getInt("id"), rs.getString("title"), rs.getString("author"),
                rs.getString("genre"), rs.getInt("total_copies"), rs.getInt("available_copies"));
    }

    @Override
    public void add(Book b) throws LibraryException {
        DBConnection.execute("INSERT INTO books(title,author,genre,total_copies,available_copies) VALUES(?,?,?,?,?)",
                b.title(), b.author(), b.genre(), b.totalCopies(), b.totalCopies());
    }

    /** Changing total copies also shifts available copies by the same amount. */
    @Override
    public void update(Book b) throws LibraryException {
        DBConnection.execute("UPDATE books SET title=?, author=?, genre=?, "
                + "available_copies = available_copies + (? - total_copies), total_copies=? WHERE id=?",
                b.title(), b.author(), b.genre(), b.totalCopies(), b.totalCopies(), b.id());
    }

    @Override
    public void delete(int id) throws LibraryException {
        DBConnection.execute("DELETE FROM books WHERE id=?", id);
    }

    @Override
    public Book findById(int id) throws LibraryException {
        List<Book> r = DBConnection.query("SELECT * FROM books WHERE id=?", BookDAO::map, id);
        if (r.isEmpty()) throw new LibraryException("Book ID " + id + " not found");
        return r.get(0);
    }

    @Override
    public List<Book> findAll() throws LibraryException {
        return DBConnection.query("SELECT * FROM books ORDER BY title", BookDAO::map);
    }

    @Override
    public List<Book> search(String field, String keyword) throws LibraryException {
        // Column name is whitelisted (never concatenate user input into SQL)
        String column = switch (field.toLowerCase()) {
            case "author" -> "author";
            case "genre" -> "genre";
            default -> "title";
        };
        return DBConnection.query("SELECT * FROM books WHERE " + column + " LIKE ? ORDER BY title",
                BookDAO::map, "%" + keyword + "%");
    }

    public void logSearch(int memberId, String text) throws LibraryException {
        DBConnection.execute("INSERT INTO search_history(member_id,query_text) VALUES(?,?)", memberId, text);
    }

    /** Returns {query, timestamp} pairs, newest first. */
    public List<String[]> searchHistory(int memberId) throws LibraryException {
        return DBConnection.query(
                "SELECT query_text, searched_at FROM search_history WHERE member_id=? ORDER BY id DESC LIMIT 100",
                rs -> new String[]{rs.getString(1), rs.getTimestamp(2).toString()}, memberId);
    }
}
