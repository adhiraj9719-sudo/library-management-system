package com.library.model;

public class Librarian extends Person {
    public Librarian(int id, String name, String email, String passwordHash) {
        super(id, name, email, passwordHash);
    }

    @Override
    public String getRole() { return "LIBRARIAN"; }
}
