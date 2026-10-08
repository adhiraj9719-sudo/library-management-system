package com.library.exception;

/** Base checked exception for all library-related errors. */
public class LibraryException extends Exception {
    public LibraryException(String message) { super(message); }
    public LibraryException(String message, Throwable cause) { super(message, cause); }
}
