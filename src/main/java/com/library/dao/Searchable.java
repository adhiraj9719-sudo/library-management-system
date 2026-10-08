package com.library.dao;

import com.library.exception.LibraryException;
import java.util.List;

public interface Searchable<T> {
    List<T> search(String field, String keyword) throws LibraryException;
}
