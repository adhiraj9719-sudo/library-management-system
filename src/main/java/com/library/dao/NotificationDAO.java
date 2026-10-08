package com.library.dao;

import com.library.exception.LibraryException;
import com.library.model.Notification;
import java.util.List;

public class NotificationDAO {

    /** Sends to selected members (respecting their notification preference). Returns how many received it. */
    public int sendTo(List<Integer> memberIds, String message) throws LibraryException {
        int sent = 0;
        for (int id : memberIds) {
            sent += DBConnection.execute(
                "INSERT INTO notifications(member_id,message) SELECT id, ? FROM users "
              + "WHERE id=? AND role='MEMBER' AND notify_enabled=TRUE", message, id);
        }
        return sent;
    }

    public int broadcast(String message) throws LibraryException {
        return DBConnection.execute("INSERT INTO notifications(member_id,message) "
                + "SELECT id, ? FROM users WHERE role='MEMBER' AND notify_enabled=TRUE", message);
    }

    public boolean alreadySent(int memberId, String message) throws LibraryException {
        return DBConnection.query("SELECT COUNT(*) FROM notifications WHERE member_id=? AND message=?",
                rs -> rs.getInt(1), memberId, message).get(0) > 0;
    }

    public List<Notification> findByMember(int memberId) throws LibraryException {
        return DBConnection.query("SELECT id, member_id, message, created_at, is_read FROM notifications "
                + "WHERE member_id=? ORDER BY id DESC LIMIT 100",
                rs -> new Notification(rs.getInt(1), rs.getInt(2), rs.getString(3),
                        rs.getTimestamp(4).toLocalDateTime(), rs.getBoolean(5)), memberId);
    }

    public int unreadCount(int memberId) throws LibraryException {
        return DBConnection.query("SELECT COUNT(*) FROM notifications WHERE member_id=? AND is_read=FALSE",
                rs -> rs.getInt(1), memberId).get(0);
    }

    public void markAllRead(int memberId) throws LibraryException {
        DBConnection.execute("UPDATE notifications SET is_read=TRUE WHERE member_id=?", memberId);
    }
}
