package com.pes.marketplace.patterns.factory.model;

public class SellerRole extends UserRole {

    public SellerRole() {
        super("Seller", "/seller/dashboard", new String[]{"list items", "update listings", "remove listings"});
    }
}
