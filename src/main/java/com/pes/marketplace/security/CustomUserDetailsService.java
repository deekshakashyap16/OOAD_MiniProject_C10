package com.pes.marketplace.security;

import com.pes.marketplace.model.User;
import com.pes.marketplace.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bridges our {@link User} entity to Spring Security's {@link UserDetailsService}.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : One job only — load a UserDetails from our DB so Spring Security can
 *       authenticate the request.
 * OCP : Adding LDAP / OAuth means a new impl, not modifying this class.
 * DIP : Depends on UserRepository interface, never on a concrete Hibernate class.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Indirection (Adapter) : Adapts our User entity to the UserDetails contract
 *                         that Spring Security requires — the two sides stay
 *                         completely decoupled from each other.
 * Low Coupling          : Spring Security knows nothing about our User entity;
 *                         this single class is the only bridge.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Invoked by Spring Security on every login attempt.
     * The "username" parameter carries the submitted email address.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found for: " + email));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("Account deactivated: " + email);
        }

        // "ROLE_" prefix is required by Spring Security's hasRole() / hasAuthority() checks
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
