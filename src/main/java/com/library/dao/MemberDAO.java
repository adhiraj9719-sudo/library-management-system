package com.library.dao;

import com.library.exception.AuthenticationException;
import com.library.exception.LibraryException;
import com.library.model.Librarian;
import com.library.model.Member;
import com.library.model.Person;
import com.library.util.PasswordUtil;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MemberDAO implements GenericDAO<Member> {

    private static Member map(ResultSet rs) throws SQLException {
        return new Member(rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                rs.getString("password_hash"), rs.getBoolean("notify_enabled"));
    }

    @Override
    public void add(Member m) throws LibraryException {
        DBConnection.execute(
                "INSERT INTO users(name,email,password_hash,role,notify_enabled) VALUES(?,?,?,'MEMBER',?)",
                m.getName(), m.getEmail(), m.getPasswordHash(), m.isNotifyEnabled());
    }

    @Override
    public void update(Member m) throws LibraryException {
        DBConnection.execute("UPDATE users SET name=?, email=?, notify_enabled=? WHERE id=? AND role='MEMBER'",
                m.getName(), m.getEmail(), m.isNotifyEnabled(), m.getId());
    }

    public void updatePassword(int id, String rawPassword) throws LibraryException {
        DBConnection.execute("UPDATE users SET password_hash=? WHERE id=?", PasswordUtil.hash(rawPassword), id);
    }

    @Override
    public void delete(int id) throws LibraryException {
        DBConnection.execute("DELETE FROM users WHERE id=? AND role='MEMBER'", id);
    }

    @Override
    public Member findById(int id) throws LibraryException {
        List<Member> r = DBConnection.query("SELECT * FROM users WHERE id=? AND role='MEMBER'", MemberDAO::map, id);
        if (r.isEmpty()) throw new LibraryException("Member ID " + id + " not found");
        return r.get(0);
    }

    @Override
    public List<Member> findAll() throws LibraryException {
        return DBConnection.query("SELECT * FROM users WHERE role='MEMBER' ORDER BY name", MemberDAO::map);
    }

    /** Logs a user in and returns the right subclass (Polymorphism). */
    public Person authenticate(String email, String rawPassword) throws LibraryException {
        List<Person> r = DBConnection.query(
                "SELECT * FROM users WHERE email=? AND password_hash=?",
                rs -> "LIBRARIAN".equals(rs.getString("role"))
                        ? new Librarian(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("password_hash"))
                        : (Person) map(rs),
                email, PasswordUtil.hash(rawPassword));
        if (r.isEmpty()) throw new AuthenticationException("Invalid email or password");
        return r.get(0);
    }
}
