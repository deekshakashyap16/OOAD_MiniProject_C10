package com.pes.marketplace.service.impl;

import com.pes.marketplace.dto.RegisterRequest;
import com.pes.marketplace.exception.DuplicateEmailException;
import com.pes.marketplace.exception.ResourceNotFoundException;
import com.pes.marketplace.model.Role;
import com.pes.marketplace.model.User;
import com.pes.marketplace.patterns.factory.UserRoleResolver;
import com.pes.marketplace.patterns.factory.model.UserRole;
import com.pes.marketplace.repository.UserRepository;
import com.pes.marketplace.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Concrete implementation of {@link AuthService}.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only user/auth business logic — no item, order, or category work here.
 * OCP : New auth features (2FA, OAuth) are added by extending, not modifying.
 * LSP : Fully substitutable wherever AuthService is declared.
 * DIP : Depends on UserRepository (interface) and PasswordEncoder (interface).
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Creator          : This class creates User objects — it has all required
 *                    data (name, email, encoded password, role) at creation time.
 * Information Expert : Knows how to validate uniqueness and encode passwords
 *                      because it owns UserRepository and PasswordEncoder.
 * Low Coupling     : References only UserRepository; never touches Item or Order.
 * High Cohesion    : Every method is a user-identity operation.
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final UserRoleResolver  userRoleResolver;   // Factory pattern entry point

    /**
     * Constructor injection — satisfies SOLID DIP and makes unit-testing trivial
     * (pass mocks for both collaborators).
     */
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserRoleResolver userRoleResolver) {
        this.userRepository   = userRepository;
        this.passwordEncoder  = passwordEncoder;
        this.userRoleResolver = userRoleResolver;
    }

    // ── UC: Register ────────────────────────────────────────────────────────

    /**
     * Registers a new Buyer or Seller.
     *
     * GRASP Creator    : AuthServiceImpl creates the User because it has all the
     *                    initialisation data from RegisterRequest.
     * GRASP Info Expert: "Does this email exist?" — UserRepository is the expert.
     */
    @Override
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(
                    "An account with email '" + request.getEmail() + "' already exists.");
        }
        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),   // never store plaintext
                request.getRole()
        );
        User saved = userRepository.save(user);

        // Factory pattern in action: the correct UserRoleFactory is picked,
        // its create() template method runs verifyRole → loadPermissions →
        // redirectToDashboard, and we hand the prepared UserRole back to the
        // server log.  Existing callers never instantiate a role themselves.
        UserRole roleConfig = userRoleResolver.resolve(saved.getRole());
        System.out.printf("[Factory] Registered %s — role=%s, dashboard=%s%n",
                saved.getEmail(), roleConfig.getRoleName(), roleConfig.getDashboardUrl());

        return saved;
    }

    // ── Lookups (read-only — no DB write, so use readOnly=true for optimisation) ──

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    // ── Admin: Deactivate ───────────────────────────────────────────────────

    /**
     * Soft-deletes a user: sets active = false so login fails,
     * but historical orders/listings remain intact for audit purposes.
     *
     * SOLID SRP  : Deactivation is a user-identity concern → belongs here.
     * GRASP Info : UserRepository is the expert on whether the user exists.
     */
    @Override
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
        user.setActive(false);
        userRepository.save(user);
    }
}
