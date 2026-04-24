package com.pes.marketplace.controller;

import com.pes.marketplace.exception.ItemNotAvailableException;
import com.pes.marketplace.exception.UnauthorizedActionException;
import com.pes.marketplace.model.Item;
import com.pes.marketplace.model.Order;
import com.pes.marketplace.model.User;
import com.pes.marketplace.service.AuthService;
import com.pes.marketplace.service.MarketplaceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Handles all HTTP interactions for the Buyer role.
 *
 * Use Cases covered
 * ─────────────────
 *  UC2  Search & Browse Items  (Major)
 *  UC3  Purchase Item          (Major)
 *  UC6  View Purchase History  (Minor)
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only Buyer HTTP flow — no Seller or Admin logic touches this class.
 * DIP : Depends on MarketplaceService and AuthService interfaces only.
 * OCP : New buyer features (wishlist, reviews) add new methods without
 *       modifying existing ones.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Controller (GRASP) : Receives buyer requests; delegates every computation
 *                      to MarketplaceService — zero business logic here.
 * Low Coupling       : No direct JPA/repository calls; never touches ItemRepository.
 * High Cohesion      : All methods are buyer-domain concerns.
 * Protected Variation: Purchase errors (unavailable, self-buy) are caught here
 *                      and shown as friendly flash messages — the UI is shielded
 *                      from internal exception types.
 *
 * URL mapping summary
 * ───────────────────
 *  GET  /buyer/browse            → UC2 search/browse page
 *  GET  /buyer/item/{id}         → item detail page
 *  POST /buyer/purchase/{itemId} → UC3 purchase
 *  GET  /buyer/orders            → UC6 purchase history
 */

@Controller
@RequestMapping("/buyer")
public class BuyerController {

    private final MarketplaceService marketplaceService;
    private final AuthService        authService;

    public BuyerController(MarketplaceService marketplaceService,
                           AuthService authService) {
        this.marketplaceService = marketplaceService;
        this.authService        = authService;
    }

    // ── Shared helper ────────────────────────────────────────────────────────

    /**
     * Resolves the Spring Security principal to our domain User entity.
     *
     * GRASP Information Expert : AuthService is the expert on user lookups.
     */
    private User currentUser(UserDetails principal) {
        return authService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in DB: " + principal.getUsername()));
    }

    // ── UC2 : Search & Browse Items ──────────────────────────────────────────

    /**
     * Renders the browse/search page.
     * All query parameters are optional — omitting them shows all APPROVED items.
     *
     * @param keyword    full-text search on name + description
     * @param categoryId filter to one category
     * @param minPrice   lower price bound
     * @param maxPrice   upper price bound
     */
    @GetMapping("/browse")
    public String browse(
            @RequestParam(required = false) String     keyword,
            @RequestParam(required = false) Long       categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Model model) {

        List<Item> items = marketplaceService.searchItems(keyword, categoryId, minPrice, maxPrice);

        model.addAttribute("items",              items);
        model.addAttribute("categories",         marketplaceService.getAllCategories());
        model.addAttribute("keyword",            keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("minPrice",           minPrice);
        model.addAttribute("maxPrice",           maxPrice);

        return "items";   // → templates/items.html
    }

    // ── Item detail page ─────────────────────────────────────────────────────

    /**
     * Shows full detail for one approved item, including the Buy button.
     * Redirects to browse if the item does not exist or is not available.
     */
    @GetMapping("/item/{id}")
    public String itemDetail(@PathVariable Long id, Model model) {
        Optional<Item> optional = marketplaceService.findItemById(id);

        if (optional.isEmpty() || !optional.get().isAvailable()) {
            return "redirect:/buyer/browse";
        }

        model.addAttribute("item", optional.get());
        return "item-detail";   // → templates/item-detail.html
    }

    // ── UC3 : Purchase Item ──────────────────────────────────────────────────

    /**
     * Processes a purchase request.
     *
     * Template method (delegated to MarketplaceServiceImpl):
     *   1. Verify item is APPROVED
     *   2. Prevent self-purchase
     *   3. Create Order with price snapshot
     *   4. Mark item SOLD  (all atomic via @Transactional)
     *
     * GRASP Protected Variation : Business-rule exceptions are caught here and
     *                             converted to user-friendly flash messages.
     */
    @PostMapping("/purchase/{itemId}")
    public String purchase(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes flash) {

        User buyer = currentUser(principal);

        try {
            Order order = marketplaceService.purchaseItem(itemId, buyer);
            flash.addFlashAttribute("successMessage",
                    "Purchase successful! Order #" + order.getId() + " confirmed.");
            return "redirect:/buyer/orders";

        } catch (ItemNotAvailableException | UnauthorizedActionException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/buyer/item/" + itemId;
        }
    }

    // ── UC6 : View Purchase History ──────────────────────────────────────────

    /**
     * Lists every order placed by the logged-in buyer, newest first.
     */
    @GetMapping("/orders")
    public String purchaseHistory(
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        User buyer = currentUser(principal);
        List<Order> orders = marketplaceService.getPurchaseHistory(buyer);
        model.addAttribute("orders", orders);
        return "purchase-history";   // → templates/purchase-history.html
    }
}
