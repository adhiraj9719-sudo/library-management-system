package com.library.model;

public class Member extends Person {
    private boolean notifyEnabled;

    public Member(int id, String name, String email, String passwordHash, boolean notifyEnabled) {
        super(id, name, email, passwordHash);
        this.notifyEnabled = notifyEnabled;
    }

    public boolean isNotifyEnabled() { return notifyEnabled; }
    public void setNotifyEnabled(boolean notifyEnabled) { this.notifyEnabled = notifyEnabled; }

    @Override
    public String getRole() { return "MEMBER"; }

    /** Used by combo boxes in the UI. */
    @Override
    public String toString() { return getName() + " (" + getEmail() + ")"; }
}
