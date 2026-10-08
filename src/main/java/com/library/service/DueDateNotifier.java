package com.library.service;

import com.library.dao.NotificationDAO;
import com.library.dao.TransactionDAO;
import com.library.exception.LibraryException;
import com.library.model.LoanRecord;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Background thread that creates due-date reminders for loans due within 2 days (or overdue). */
public class DueDateNotifier implements Runnable {
    private static final int WARN_DAYS = 2;

    private final TransactionDAO transactions = new TransactionDAO();
    private final NotificationDAO notifications = new NotificationDAO();
    private ScheduledExecutorService scheduler;

    /** Starts a daemon thread that runs now and then every {@code periodMinutes}. */
    public synchronized void start(long periodMinutes) {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "due-date-notifier");
            t.setDaemon(true);                 // never blocks JVM shutdown
            return t;
        });
        scheduler.scheduleAtFixedRate(this, 0, periodMinutes, TimeUnit.MINUTES);
    }

    public synchronized void stop() {
        if (scheduler != null) scheduler.shutdownNow();
        scheduler = null;
    }

    @Override
    public void run() {
        try {
            runOnce();
        } catch (LibraryException e) {
            System.err.println("[DueDateNotifier] " + e.getMessage());   // keep the scheduler alive
        }
    }

    /** One pass; returns how many reminders were created. Also callable from the librarian UI. */
    public synchronized int runOnce() throws LibraryException {
        int created = 0;
        List<LoanRecord> due = transactions.findDueSoon(WARN_DAYS);
        for (LoanRecord loan : due) {
            String msg = (loan.isOverdue() ? "OVERDUE: '" : "Reminder: '") + loan.bookTitle()
                    + "' is due on " + loan.dueDate() + ".";
            if (!notifications.alreadySent(loan.memberId(), msg)) {
                created += notifications.sendTo(List.of(loan.memberId()), msg);
            }
        }
        return created;
    }
}
