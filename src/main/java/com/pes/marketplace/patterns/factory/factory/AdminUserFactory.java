package com.pes.marketplace.patterns.factory.factory;

import com.pes.marketplace.patterns.factory.model.*;

public class AdminUserFactory extends UserRoleFactory {

    @Override
    protected UserRole retrieveRole(String type) {
        switch (type.toLowerCase()) {
            case "admin":
                return new AdminRole();
            default:
                throw new IllegalArgumentException("Unknown admin type: " + type);
        }
    }
}
