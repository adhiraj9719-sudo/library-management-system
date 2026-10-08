package com.library.dao;

import com.library.exception.LibraryException;
import java.util.List;

/** Generic CRUD contract implemented by every DAO (Interfaces + Generics). */
public interface GenericDAO<T> {
    void add(T item) throws LibraryException;
    void update(T item) throws LibraryException;
    void delete(int id) throws LibraryException;
    T findById(int id) throws LibraryException;
    List<T> findAll() throws LibraryException;
}
