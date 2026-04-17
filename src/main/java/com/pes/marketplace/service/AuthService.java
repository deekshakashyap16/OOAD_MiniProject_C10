package com.pes.marketplace.service;

import com.pes.marketplace.dto.RegisterRequest;
import com.pes.marketplace.model.Role;
import com.pes.marketplace.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Contract for all user-identity and account-management operations.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only user/auth concerns; item and order logic live in MarketplaceService.
 * ISP : Fine-grained interface — callers (controllers) only see what they need.
 * DIP : Controllers inject this interface; Spring provides the impl at runtime.
 * OCP : A new OAuth2 impl can be dropped in without touching this contract.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Controller (GRASP role) : Coordinates all user-identity use cases.
 * Low Coupling            : MarketplaceService never depends on AuthService.
 */
public interface AuthService {

    /**
     * UC: Register — create a new Buyer or Seller account.
     *
     * @throws com.pes.marketplace.exception.DuplicateEmailException if email taken
     */
    User register(RegisterRequest request);

    /** Look up a user by email — used by Spring Security and controllers. */
    Optional<User> findByEmail(String email);

    /** Look up a user by primary key — used by controllers to get current user entity. */
    Optional<User> findById(Long id);

    /** Admin: list all users regardless of role. */
    List<User> findAllUsers();

    /** Admin: list users filtered by role (BUYER / SELLER / ADMIN). */
    List<User> findByRole(Role role);

    /**
     * Admin: soft-deactivate a user (sets active = false).
     * Historical orders and listings are preserved; user can no longer log in.
     */
    void deactivateUser(Long userId);
}
