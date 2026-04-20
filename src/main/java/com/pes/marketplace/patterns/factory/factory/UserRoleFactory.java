package com.pes.marketplace.patterns.factory.factory;

import com.pes.marketplace.patterns.factory.model.UserRole;

public abstract class UserRoleFactory {

    public UserRole create(String type) {
        UserRole role = retrieveRole(type);
        prepareRole(role);
        return role;
    }

    protected abstract UserRole retrieveRole(String type);

    private void prepareRole(UserRole role) {
        if (role != null) {
            role.verifyRole();
            role.loadPermissions();
            role.redirectToDashboard();
        }
    }
}
