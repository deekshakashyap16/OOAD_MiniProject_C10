package com.pes.marketplace.controller;

import com.pes.marketplace.dto.ItemRequest;
import com.pes.marketplace.exception.UnauthorizedActionException;
import com.pes.marketplace.model.Item;
import com.pes.marketplace.model.User;
import com.pes.marketplace.service.AuthService;
import com.pes.marketplace.service.MarketplaceService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Handles all HTTP interactions for the Seller role.
 *
 * Use Cases covered
 * ─────────────────
 *  UC1  List Item for Sale   (Major)
 *  UC5  Update Item Details  (Minor)
 *  UC5  Remove Listing       (Minor)
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only Seller HTTP flow — Buyer/Admin concerns are in separate controllers.
 * DIP : Depends on MarketplaceService and AuthService interfaces, not impls.
 * OCP : New seller features add new methods; existing ones are untouched.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Controller (GRASP) : Receives seller HTTP requests; delegates all work
 *                      to MarketplaceService — zero business logic here.
 * Low Coupling       : No direct use of ItemRepository, JPA, or BCrypt.
 * High Cohesion      : Every method is a seller-domain operation.
 * Protected Variation: Ownership and state errors from the service layer are
 *                      caught and shown as flash messages — the template never
 *                      sees raw exceptions.
 *
 * URL mapping summary
 * ───────────────────
 *  GET  /seller/dashboard        → seller's own listings
 *  GET  /seller/item/new         → blank listing form (UC1)
 *  POST /seller/item/new         → submit new listing (UC1)
 *  GET  /seller/item/{id}/edit   → pre-filled edit form (UC5)
 *  POST /seller/item/{id}/edit   → submit update (UC5)
 *  POST /seller/item/{id}/remove → remove listing (UC5)
 */
@Controller
@RequestMapping("/seller")
public class SellerController {

    private final MarketplaceService marketplaceService;
    private final AuthService        authService;

    public SellerController(MarketplaceService marketplaceService,
                            AuthService authService) {
        this.marketplaceService = marketplaceService;
        this.authService        = authService;
    }

    // ── Shared helper ────────────────────────────────────────────────────────

    private User currentUser(UserDetails principal) {
        return authService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + principal.getUsername()));
    }

    // ── Seller Dashboard ─────────────────────────────────────────────────────

    /**
     * Shows all items listed by this seller across all statuses
     * (PENDING_REVIEW, APPROVED, REJECTED, SOLD, REMOVED).
     * Sellers can see admin feedback for rejected items here.
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        User seller = currentUser(principal);
        List<Item> items = marketplaceService.getItemsBySeller(seller);
        model.addAttribute("items",  items);
        model.addAttribute("seller", seller);
        return "seller-items";   // → templates/seller-items.html
    }

    // ── UC1 : List Item for Sale ─────────────────────────────────────────────

    /**
     * Renders a blank "Add Item" form with the category drop-down populated.
     */
    @GetMapping("/item/new")
    public String newItemForm(Model model) {
        model.addAttribute("itemRequest", new ItemRequest());
        model.addAttribute("categories",  marketplaceService.getAllCategories());
        return "add-item";   // → templates/add-item.html
    }

    /**
     * Processes the new-listing submission.
     *
     * On success: item is saved in PENDING_REVIEW state and the seller is
     * redirected to their dashboard with a confirmation message.
     *
     * GRASP Creator    : MarketplaceService creates the Item (not this controller).
     * GRASP Low Coupling: Controller never touches ItemRepository.
     */
    @PostMapping("/item/new")
    public String createItem(
            @Valid @ModelAttribute("itemRequest") ItemRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", marketplaceService.getAllCategories());
            return "add-item";
        }

        User seller = currentUser(principal);
        marketplaceService.listItem(request, seller);
        flash.addFlashAttribute("successMessage",
                "Item listed successfully! It is now pending admin review.");
        return "redirect:/seller/dashboard";
    }

    // ── UC5 : Update Item Details ────────────────────────────────────────────

    /**
     * Renders the edit form pre-filled with the existing item data.
     * Redirects to dashboard if the item belongs to a different seller.
     */
    @GetMapping("/item/{id}/edit")
    public String editItemForm(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Model model,
            RedirectAttributes flash) {

        User seller = currentUser(principal);
        Item item   = marketplaceService.findItemById(id)
                        .orElse(null);

        if (item == null || !item.getSeller().getId().equals(seller.getId())) {
            flash.addFlashAttribute("errorMessage", "Item not found or access denied.");
            return "redirect:/seller/dashboard";
        }

        // Pre-populate DTO from existing item data
        ItemRequest request = new ItemRequest();
        request.setName(item.getName());
        request.setDescription(item.getDescription());
        request.setPrice(item.getPrice());
        request.setCategoryId(item.getCategory().getId());

        model.addAttribute("itemRequest", request);
        model.addAttribute("item",        item);
        model.addAttribute("categories",  marketplaceService.getAllCategories());
        return "add-item";   // reuse the same form template; th:if on item != null shows edit title
    }

    /**
     * Processes the edit submission.
     * After a successful update the item is reset to PENDING_REVIEW for re-approval.
     *
     * GRASP Protected Variation : Ownership and state-transition rules are
     *                             enforced inside MarketplaceService.updateItem(),
     *                             not here — controller stays thin.
     */
    @PostMapping("/item/{id}/edit")
    public String updateItem(
            @PathVariable Long id,
            @Valid @ModelAttribute("itemRequest") ItemRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", marketplaceService.getAllCategories());
            // keep item for heading in template
            marketplaceService.findItemById(id)
                    .ifPresent(i -> model.addAttribute("item", i));
            return "add-item";
        }

        User seller = currentUser(principal);
        try {
            marketplaceService.updateItem(id, request, seller);
            flash.addFlashAttribute("successMessage",
                    "Item updated and resubmitted for admin review.");
        } catch (UnauthorizedActionException | IllegalStateException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/seller/dashboard";
    }

    // ── UC5 : Remove Listing ─────────────────────────────────────────────────

    /**
     * Soft-removes a listing (sets status = REMOVED).
     * Sold items cannot be removed — the service enforces this rule.
     */
    @PostMapping("/item/{id}/remove")
    public String removeItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes flash) {

        User seller = currentUser(principal);
        try {
            marketplaceService.removeItem(id, seller);
            flash.addFlashAttribute("successMessage", "Listing removed successfully.");
        } catch (UnauthorizedActionException | IllegalStateException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/seller/dashboard";
    }
}
