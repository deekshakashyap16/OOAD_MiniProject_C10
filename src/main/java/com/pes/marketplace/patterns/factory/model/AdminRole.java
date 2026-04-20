package com.pes.marketplace.patterns.factory.model;

public class AdminRole extends UserRole {

    public AdminRole() {
        super("Admin", "/admin/dashboard", new String[]{"moderate items", "manage users", "view reports", "manage categories"});
    }
}
