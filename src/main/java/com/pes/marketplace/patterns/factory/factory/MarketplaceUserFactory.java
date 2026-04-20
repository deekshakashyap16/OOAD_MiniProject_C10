package com.pes.marketplace.patterns.factory.factory;

import com.pes.marketplace.patterns.factory.model.*;

public class MarketplaceUserFactory extends UserRoleFactory {

    @Override
    protected UserRole retrieveRole(String type) {
        switch (type.toLowerCase()) {
            case "buyer":
                return new BuyerRole();
            case "seller":
                return new SellerRole();
            default:
                throw new IllegalArgumentException("Unknown marketplace user type: " + type);
        }
    }
}
