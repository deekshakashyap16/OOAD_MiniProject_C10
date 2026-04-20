package com.pes.marketplace.patterns.factory.model;

public class BuyerRole extends UserRole {

    public BuyerRole() {
        super("Buyer", "/buyer/browse", new String[]{"browse items", "purchase items", "view order history"});
    }
}
