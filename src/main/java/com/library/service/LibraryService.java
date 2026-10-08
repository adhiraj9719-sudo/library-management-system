package com.library.service;

import com.library.dao.TransactionDAO;
import com.library.exception.LibraryException;

/**
 * Business rules for borrowing and returning.
 * Several GUI windows (librarian + members) and background threads may call this at once,
 * so the critical sections are synchronized on one shared lock. The DAO additionally
 * uses SELECT ... FOR UPDATE so the database itself stays consistent.
 */
public class LibraryService {
    public static final int MAX_ACTIVE_LOANS = 5;
    private static final Object LOCK = new Object();

    private final TransactionDAO transactions = new TransactionDAO();

    public void borrowBook(int bookId, int memberId) throws LibraryException {
        synchronized (LOCK) {
            transactions.borrow(bookId, memberId, MAX_ACTIVE_LOANS);
        }
    }

    public void returnBook(int bookId, int memberId) throws LibraryException {
        synchronized (LOCK) {
            transactions.giveBack(bookId, memberId);
        }
    }
}
