package com.library.model;

/** Abstract base class for every user of the system (Inheritance + Abstraction). */
public abstract class Person {
    private final int id;
    private String name;
    private String email;
    private String passwordHash;

    protected Person(int id, String name, String email, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** Each subclass reports its own role (Polymorphism). */
    public abstract String getRole();

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    @Override
    public String toString() { return getRole() + "[" + id + ", " + name + "]"; }
}
