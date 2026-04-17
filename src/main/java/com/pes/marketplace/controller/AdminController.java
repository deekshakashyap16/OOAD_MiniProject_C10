package com.pes.marketplace.controller;

import com.pes.marketplace.dto.ReportDto;
import com.pes.marketplace.model.ItemStatus;
import com.pes.marketplace.service.AuthService;
import com.pes.marketplace.service.MarketplaceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles all HTTP interactions for the Admin role.
 *
 * Use Cases covered
 * ─────────────────
 *  UC4  Moderate Listings              (Major)
 *  UC7  Send Price Modification Feedback (Minor)
 *  UC8  Generate Reports               (Minor)
 *  +    Manage Categories
 *  +    Manage Users (deactivate)
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only Admin HTTP flow — Buyer/Seller concerns are separate controllers.
 * DIP : Depends on MarketplaceService and AuthService interfaces only.
 * OCP : New admin features (e.g. featured listings) add new methods; existing
 *       methods are never modified.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Controller (GRASP) : First receiver of admin HTTP requests; delegates all
 *                      computation to service layer — zero business logic here.
 * Facade             : Acts as a single entry-point over MarketplaceService
 *                      and AuthService — admin pages need both.
 * Low Coupling       : No direct JPA calls; never sees ItemRepository.
 * High Cohesion      : All methods are admin-domain concerns.
 * Protected Variation: Service exceptions are caught and shown as flash messages.
 *
 * URL mapping summary
 * ───────────────────
 *  GET  /admin/dashboard                    → overview widgets
 *  GET  /admin/moderate                     → UC4 pending items queue
 *  POST /admin/moderate/{id}/approve        → UC4 approve
 *  POST /admin/moderate/{id}/reject         → UC4 reject
 *  GET  /admin/items                        → all APPROVED items
 *  POST /admin/item/{id}/price-feedback     → UC7
 *  GET  /admin/reports                      → UC8
 *  GET  /admin/categories                   → category list
 *  POST /admin/categories/new               → create category
 *  POST /admin/categories/{id}/delete       → delete category
 *  GET  /admin/users                        → user list
 *  POST /admin/users/{id}/deactivate        → deactivate user
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final MarketplaceService marketplaceService;
    private final AuthService        authService;

    public AdminController(MarketplaceService marketplaceService,
                           AuthService authService) {
        this.marketplaceService = marketplaceService;
        this.authService        = authService;
    }

    // ── Admin Dashboard ──────────────────────────────────────────────────────

    /**
     * Renders the admin dashboard with high-level counts for quick overview.
     * Each metric is fetched via the service layer — no raw DB calls here.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pendingCount",  marketplaceService.countItemsByStatus(ItemStatus.PENDING_REVIEW));
        model.addAttribute("approvedCount", marketplaceService.countItemsByStatus(ItemStatus.APPROVED));
        model.addAttribute("soldCount",     marketplaceService.countItemsByStatus(ItemStatus.SOLD));
        model.addAttribute("orderCount",    marketplaceService.countOrders());
        return "admin-dashboard";   // → templates/admin-dashboard.html
    }

    // ── UC4 : Moderate Listings ──────────────────────────────────────────────

    /**
     * Renders the moderation queue — items in PENDING_REVIEW, oldest first.
     */
    @GetMapping("/moderate")
    public String moderateQueue(Model model) {
        model.addAttribute("items", marketplaceService.getPendingItems());
        return "admin-items";   // → templates/admin-items.html
    }

    /**
     * Approves a pending item, optionally attaching a feedback note.
     * After approval the item becomes visible to all buyers immediately.
     *
     * GRASP Protected Variation : The "only PENDING items can be approved"
     *                             rule lives in MarketplaceService, not here.
     */
    @PostMapping("/moderate/{id}/approve")
    public String approveItem(
            @PathVariable Long id,
            @RequestParam(required = false) String feedback,
            RedirectAttributes flash) {

        try {
            marketplaceService.moderateItem(id, ItemStatus.APPROVED, feedback);
            flash.addFlashAttribute("successMessage", "Item approved and now live.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/moderate";
    }

    /**
     * Rejects a pending item with mandatory feedback explaining the reason.
     * The seller will see this feedback on their dashboard and can revise + resubmit.
     */
    @PostMapping("/moderate/{id}/reject")
    public String rejectItem(
            @PathVariable Long id,
            @RequestParam(required = false) String feedback,
            RedirectAttributes flash) {

        try {
            marketplaceService.moderateItem(id, ItemStatus.REJECTED, feedback);
            flash.addFlashAttribute("successMessage", "Item rejected. Feedback sent to seller.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/moderate";
    }

    // ── UC7 : Send Price Modification Feedback ───────────────────────────────

    /**
     * Lists all currently APPROVED items — admin can pick one to send price feedback.
     */
    @GetMapping("/items")
    public String approvedItems(Model model) {
        model.addAttribute("items", marketplaceService.getAllApprovedItems());
        return "admin-approved-items";   // → templates/admin-approved-items.html
    }

    /**
     * Attaches price-modification feedback to an item and resets it to
     * PENDING_REVIEW so the seller must revise the price and resubmit.
     *
     * GRASP Protected Variation : The status reset rule lives in the service.
     */
    @PostMapping("/item/{id}/price-feedback")
    public String sendPriceFeedback(
            @PathVariable Long id,
            @RequestParam String feedback,
            RedirectAttributes flash) {

        try {
            marketplaceService.sendPriceFeedback(id, feedback);
            flash.addFlashAttribute("successMessage",
                    "Price-modification feedback sent. Item moved back to review.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/items";
    }

    // ── UC8 : Generate Reports ───────────────────────────────────────────────

    /**
     * Builds the full ReportDto in one service call and passes it to the view.
     *
     * GRASP Information Expert : Each repository is the expert for its own counts;
     *                            MarketplaceService assembles them into a ReportDto.
     */
    @GetMapping("/reports")
    public String reports(Model model) {
        ReportDto report = marketplaceService.generateReport();
        model.addAttribute("report", report);
        return "admin-reports";   // → templates/admin-reports.html
    }

    // ── Category Management ──────────────────────────────────────────────────

    /** Lists all categories with a form to add a new one. */
    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", marketplaceService.getAllCategories());
        return "admin-categories";   // → templates/admin-categories.html
    }

    /** Creates a new item category. */
    @PostMapping("/categories/new")
    public String createCategory(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes flash) {

        try {
            marketplaceService.createCategory(name, description);
            flash.addFlashAttribute("successMessage", "Category '" + name + "' created.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    /** Deletes a category by ID. Fails gracefully if it does not exist. */
    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(
            @PathVariable Long id,
            RedirectAttributes flash) {

        try {
            marketplaceService.deleteCategory(id);
            flash.addFlashAttribute("successMessage", "Category deleted.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ── User Management ──────────────────────────────────────────────────────

    /** Lists all registered users across all roles. */
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", authService.findAllUsers());
        return "admin-users";   // → templates/admin-users.html
    }

    /**
     * Soft-deactivates a user (sets active = false).
     * Their historical orders and listings are preserved for audit purposes.
     *
     * SOLID SRP : Deactivation logic lives in AuthService — admin controller
     *             just routes the HTTP request.
     */
    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(
            @PathVariable Long id,
            RedirectAttributes flash) {

        try {
            authService.deactivateUser(id);
            flash.addFlashAttribute("successMessage", "User deactivated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
