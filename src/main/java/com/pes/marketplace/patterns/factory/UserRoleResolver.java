package com.pes.marketplace.patterns.factory;

import com.pes.marketplace.model.Role;
import com.pes.marketplace.patterns.factory.factory.AdminUserFactory;
import com.pes.marketplace.patterns.factory.factory.MarketplaceUserFactory;
import com.pes.marketplace.patterns.factory.factory.UserRoleFactory;
import com.pes.marketplace.patterns.factory.model.UserRole;
import org.springframework.stereotype.Component;

/**
 * Public entry point for the Factory pattern.
 *
 * Callers (AuthServiceImpl, SecurityConfig) depend only on this component —
 * they never instantiate a UserRole or a concrete UserRoleFactory directly.
 * Adding a new role = one more subclass + one more branch here; existing
 * callers are untouched.
 */
@Component
public class UserRoleResolver {

    /**
     * Picks the correct concrete factory for the given Role and runs its
     * create() template method, which internally verifies the role,
     * loads permissions and prepares the redirect target.
     */
    public UserRole resolve(Role role) {
        UserRoleFactory factory = pickFactory(role);
        return factory.create(role.name().toLowerCase());
    }

    /**
     * Convenience for Spring Security — converts authority strings like
     * "ROLE_BUYER" into a prepared UserRole so the security layer can read
     * the dashboard URL without encoding role → URL mappings itself.
     */
    public UserRole resolveFromAuthority(String authority) {
        String clean = authority.toUpperCase().replace("ROLE_", "");
        return resolve(Role.valueOf(clean));
    }

    private UserRoleFactory pickFactory(Role role) {
        if (role == Role.ADMIN) {
            return new AdminUserFactory();
        }
        return new MarketplaceUserFactory();
    }
}
