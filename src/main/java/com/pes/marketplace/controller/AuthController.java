package com.pes.marketplace.controller;

import com.pes.marketplace.dto.RegisterRequest;
import com.pes.marketplace.exception.DuplicateEmailException;
import com.pes.marketplace.model.Role;
import com.pes.marketplace.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles user registration and the login page.
 * Spring Security intercepts POST /login itself — this controller only
 * serves the GET (render the form) for login.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only authentication-flow HTTP handling lives here; no business logic.
 * DIP : Depends on AuthService interface, not AuthServiceImpl.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Controller (GRASP role) : First receiver of auth-related HTTP requests;
 *                           delegates all real work to AuthService.
 * Low Coupling            : Zero knowledge of JPA, BCrypt, or UserRepository.
 * High Cohesion           : Every method is an auth-flow concern.
 *
 * URL mapping summary
 * ───────────────────
 *  GET  /login        → render login.html
 *  POST /login        → handled by Spring Security (not this controller)
 *  GET  /register     → render register.html
 *  POST /register     → validate + call AuthService.register()
 */
@Controller
public class AuthController {

    private final AuthService authService;

    /** Constructor injection — DIP + easy unit-testing. */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── GET /login ───────────────────────────────────────────────────────────

    /**
     * Renders the login page.
     * ?error=true  → Spring Security sets this on bad credentials.
     * ?logout=true → set by our logout success URL in SecurityConfig.
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";   // → templates/login.html
    }

    // ── GET / ────────────────────────────────────────────────────────────────

    /**
     * Root redirect: unauthenticated users go to login;
     * authenticated users are already redirected by SecurityConfig successHandler.
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    // ── GET /register ────────────────────────────────────────────────────────

    /**
     * Renders the registration form with an empty DTO and the role options
     * (BUYER / SELLER — Admins are created through DataInitializer only).
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("roles", new Role[]{ Role.BUYER, Role.SELLER });
        return "register";   // → templates/register.html
    }

    // ── POST /register ───────────────────────────────────────────────────────

    /**
     * Validates the form, calls AuthService to persist the new user,
     * then redirects to login with a success flash message.
     *
     * Validation layers:
     *  1. Bean Validation (@Valid)   — field-level constraints in RegisterRequest.
     *  2. Password-match check       — cross-field, handled here explicitly.
     *  3. Duplicate-email guard      — thrown by AuthServiceImpl.
     *
     * GRASP Creator    : AuthService (not this controller) creates the User object.
     * GRASP Low Coupling: Controller never touches UserRepository or PasswordEncoder.
     */
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes flash) {

        // Cross-field validation: passwords must match
        if (!request.passwordsMatch()) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "mismatch",
                    "Passwords do not match.");
        }

        // If any validation errors exist, re-render the form with messages
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", new Role[]{ Role.BUYER, Role.SELLER });
            return "register";
        }

        try {
            authService.register(request);
            flash.addFlashAttribute("successMessage",
                    "Account created! Please log in.");
            return "redirect:/login";

        } catch (DuplicateEmailException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            model.addAttribute("roles", new Role[]{ Role.BUYER, Role.SELLER });
            return "register";
        }
    }
}
