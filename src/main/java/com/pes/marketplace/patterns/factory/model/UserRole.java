package com.pes.marketplace.patterns.factory.model;

public abstract class UserRole {
    protected String roleName;
    protected String dashboardUrl;
    protected String[] permissions;

    public UserRole(String roleName, String dashboardUrl, String[] permissions) {
        this.roleName = roleName;
        this.dashboardUrl = dashboardUrl;
        this.permissions = permissions;
    }

    public String getRoleName()      { return roleName; }
    public String getDashboardUrl()  { return dashboardUrl; }
    public String[] getPermissions() { return permissions; }

    public void verifyRole() {
        System.out.println(getClass().getSimpleName() + " role verified");
    }

    public void loadPermissions() {
        System.out.println(getClass().getSimpleName() + " permissions loaded: " + String.join(", ", permissions));
    }

    public void redirectToDashboard() {
        System.out.println(getClass().getSimpleName() + " redirected to " + dashboardUrl);
    }

    public void accessDashboard() {
        System.out.println(getClass().getSimpleName() + " accessed dashboard as " + roleName);
    }
}
