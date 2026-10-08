package com.library.model;

import java.time.LocalDate;

/** One borrow/return transaction (joined with book title and member name for display). */
public record LoanRecord(int id, int bookId, int memberId, String bookTitle, String memberName,
                         LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate) {
    public boolean isReturned() { return returnDate != null; }
    public boolean isOverdue() { return returnDate == null && dueDate.isBefore(LocalDate.now()); }
}
