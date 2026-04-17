package com.pes.marketplace.repository;

import com.pes.marketplace.model.Role;
import com.pes.marketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence layer for {@link User}.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * DIP : AuthService depends on this interface, not on a concrete Hibernate impl.
 * ISP : Only user-relevant queries live here; item/order queries are separate.
 * SRP : Sole responsibility is user data access.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Pure Fabrication : A helper class invented purely to handle DB access cleanly
 *                    — not part of the business domain.
 * Low Coupling     : Nothing except AuthService/SecurityService depends on this.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Load a user by email — used by Spring Security during login
     * and by AuthService for profile lookup.
     */
    Optional<User> findByEmail(String email);

    /**
     * Guard against duplicate registrations (UC: Register).
     */
    boolean existsByEmail(String email);

    /**
     * Admin user-management: filter users by role (BUYER / SELLER / ADMIN).
     */
    List<User> findByRole(Role role);

    /**
     * UC8 Generate Reports — total active platform users.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();

    /**
     * UC8 Generate Reports — active users broken down by role.
     *
     * @param role BUYER, SELLER, or ADMIN
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.active = true")
    long countActiveByRole(@Param("role") Role role);
}
