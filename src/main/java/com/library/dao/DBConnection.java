package com.library.dao;

import com.library.exception.LibraryException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** JDBC connection factory plus small helpers that remove PreparedStatement boilerplate. */
public final class DBConnection {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                    "db.properties not found. Copy db.properties.example to db.properties in src/main/resources.");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() { }

    /** Maps one row of a ResultSet to an object. */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static Connection get() throws LibraryException {
        try {
            return DriverManager.getConnection(PROPS.getProperty("url"),
                    PROPS.getProperty("user"), PROPS.getProperty("password"));
        } catch (SQLException e) {
            throw new LibraryException("Database connection failed: " + e.getMessage(), e);
        }
    }

    public static int execute(String sql, Object... params) throws LibraryException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new LibraryException(friendly(e), e);
        }
    }

    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) throws LibraryException {
        List<T> out = new ArrayList<>();
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapper.map(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new LibraryException(friendly(e), e);
        }
    }

    public static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }

    private static String friendly(SQLException e) {
        String m = String.valueOf(e.getMessage());
        if (m.contains("Duplicate entry")) return "That email is already registered.";
        if (m.contains("foreign key constraint")) return "Operation blocked: this record is referenced by other records.";
        return "Database error: " + m;
    }
}
