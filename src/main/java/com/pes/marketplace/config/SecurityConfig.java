package com.pes.marketplace.config;

import com.pes.marketplace.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security configuration for Campus Marketplace.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only security wiring lives here; business logic is in services.
 * OCP : New role-based URL rules can be appended without modifying existing ones.
 * DIP : Depends on CustomUserDetailsService (an abstraction), not on the
 *       underlying UserRepository or Hibernate.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Pure Fabrication  : A configuration class that has no real-world counterpart —
 *                     it exists purely to wire the security infrastructure.
 * Protected Variation : Role-URL mappings are centralised here so that adding a
 *                       new role never requires touching any controller.
 * Low Coupling      : Controllers have zero security logic — they just declare
 *                     @RequestMapping and trust this class to protect them.
 *
 * ── Design Pattern ─────────────────────────────────────────────────────────
 * Strategy Pattern  : DaoAuthenticationProvider is a pluggable authentication
 *                     strategy. Swapping it for JwtAuthenticationProvider or
 *                     OAuth2 requires only a different @Bean here.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // ── Beans ───────────────────────────────────────────────────────────────

    /**
     * BCrypt password encoder — industry-standard adaptive hash.
     * Injected into AuthServiceImpl for encoding and here for verification.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Strategy Pattern: DaoAuthenticationProvider wires our UserDetailsService
     * to Spring Security's authentication pipeline.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes AuthenticationManager as a bean so controllers / services
     * can programmatically authenticate if needed (e.g., re-auth on password change).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Security Filter Chain ───────────────────────────────────────────────

    /**
     * Defines HTTP security rules, login / logout configuration.
     *
     * GRASP Protected Variation : All role-to-URL mappings live here.
     *                             Controllers never duplicate access-control logic.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ── Register our custom auth provider ───────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── URL access rules (order matters — most specific first) ───────
            .authorizeHttpRequests(auth -> auth

                // Public: landing, auth pages, static assets, H2 console
                .requestMatchers(
                        "/", "/login", "/register",
                        "/css/**", "/js/**", "/images/**",
                        "/h2-console/**"
                ).permitAll()

                // Role-scoped paths
                .requestMatchers("/buyer/**").hasRole("BUYER")
                .requestMatchers("/seller/**").hasRole("SELLER")
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // ── Form Login ───────────────────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")                    // our custom login page
                .loginProcessingUrl("/login")           // Spring Security handles POST /login
                .usernameParameter("email")             // matches the form field name
                .passwordParameter("password")
                // Role-based redirect after successful login
                .successHandler((request, response, authentication) -> {
                    String redirectUrl = determinePostLoginUrl(authentication);
                    response.sendRedirect(redirectUrl);
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // ── Logout ───────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // ── CSRF: disable for H2 console only ───────────────────────────
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            )

            // ── Allow H2 console frames (same origin) ────────────────────────
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    /**
     * Determines the landing page after login based on the user's role.
     *
     * GRASP Information Expert : SecurityConfig owns role-to-URL mapping,
     *                            so this logic belongs here, not in controllers.
     */
    private String determinePostLoginUrl(
            org.springframework.security.core.Authentication authentication) {

        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .map(role -> switch (role) {
                    case "ROLE_BUYER"  -> "/buyer/browse";
                    case "ROLE_SELLER" -> "/seller/dashboard";
                    case "ROLE_ADMIN"  -> "/admin/dashboard";
                    default            -> "/login";
                })
                .findFirst()
                .orElse("/login");
    }
}
