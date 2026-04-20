package com.pes.marketplace.service;

import com.pes.marketplace.dto.ItemRequest;
import com.pes.marketplace.dto.ReportDto;
import com.pes.marketplace.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Contract for every marketplace operation: listings, purchases, moderation,
 * category management, and reporting.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Handles only marketplace business logic; user/auth is in AuthService.
 * DIP : All four controllers depend on this interface, never on the concrete impl.
 * OCP : New use cases (e.g. auction, wishlist) are added here and implemented in
 *       MarketplaceServiceImpl without touching existing controllers.
 * ISP : Although a single interface, the method groupings mirror each actor's
 *       use cases — Buyer / Seller / Admin — keeping concerns clearly separated.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Controller (GRASP) : Coordinates all marketplace use cases; delegates
 *                      persistence to repositories.
 * High Cohesion      : All methods relate to the campus-marketplace domain.
 * Low Coupling       : Controllers talk to this one interface; they need no
 *                      knowledge of JPA, H2, or any concrete class.
 * Protected Variation: State-transition rules (PENDING→APPROVED, etc.) are
 *                      hidden inside the implementation — callers just call
 *                      moderateItem() and trust the rules are enforced.
 */
public interface MarketplaceService {

    // ══════════════════════════════════════════════════════════════════════
    // ITEM — Seller use cases
    // ══════════════════════════════════════════════════════════════════════

    /**
     * UC1 List Item for Sale.
     * Creates a new listing in PENDING_REVIEW state so the admin can approve it.
     *
     * @param request form data from the seller
     * @param seller  the authenticated seller user
     * @return the persisted {@link Item}
     */
    Item listItem(ItemRequest request, User seller);

    /**
     * UC5 Update Item Details.
     * Seller may only edit their own items that are not SOLD or REMOVED.
     * After update the item is reset to PENDING_REVIEW.
     *
     * @throws com.pes.marketplace.exception.UnauthorizedActionException if not owner
     * @throws com.pes.marketplace.exception.ResourceNotFoundException   if item missing
     */
    Item updateItem(Long itemId, ItemRequest request, User seller);

    /**
     * UC5 (minor) Remove Listing.
     * Sets status to REMOVED. Cannot be undone.
     *
     * @throws com.pes.marketplace.exception.UnauthorizedActionException if not owner
     */
    void removeItem(Long itemId, User seller);

    /** Seller Dashboard — items owned by this seller in all statuses. */
    List<Item> getItemsBySeller(User seller);

    /**
     * OCP entry point — returns the listing fee for the given category by
     * delegating to the CategoryPolicy hierarchy (ListingFeeCalculator).
     * New category policies are added without modifying this method.
     */
    double calculateListingFee(Category category);

    // ══════════════════════════════════════════════════════════════════════
    // ITEM — Buyer use cases
    // ══════════════════════════════════════════════════════════════════════

    /**
     * UC2 Search & Browse Items.
     * Returns only APPROVED items. All filter parameters are optional (pass null to skip).
     *
     * @param keyword    matched against name and description (case-insensitive)
     * @param categoryId filter to a specific category
     * @param minPrice   lower price bound
     * @param maxPrice   upper price bound
     */
    List<Item> searchItems(String keyword, Long categoryId,
                           BigDecimal minPrice, BigDecimal maxPrice);

    /** Browse page default view — all APPROVED items, newest first. */
    List<Item> getApprovedItems();

    /** Item detail page. */
    Optional<Item> findItemById(Long id);

    // ══════════════════════════════════════════════════════════════════════
    // ORDER — Buyer use cases
    // ══════════════════════════════════════════════════════════════════════

    /**
     * UC3 Purchase Item.
     * Verifies availability, creates an Order, marks the item SOLD — all atomically.
     *
     * @throws com.pes.marketplace.exception.ItemNotAvailableException   if not APPROVED
     * @throws com.pes.marketplace.exception.UnauthorizedActionException if self-purchase
     */
    Order purchaseItem(Long itemId, User buyer);

    /**
     * UC6 View Purchase History.
     * Returns all orders placed by this buyer, newest first.
     */
    List<Order> getPurchaseHistory(User buyer);

    // ══════════════════════════════════════════════════════════════════════
    // ITEM — Admin use cases
    // ══════════════════════════════════════════════════════════════════════

    /**
     * UC4 Moderate Listings — approve or reject a PENDING_REVIEW item.
     *
     * @param itemId   the item to moderate
     * @param newStatus must be APPROVED or REJECTED
     * @param feedback optional message shown to the seller
     * @throws IllegalStateException if item is not in PENDING_REVIEW
     */
    Item moderateItem(Long itemId, ItemStatus newStatus, String feedback);

    /**
     * UC7 Send Price Modification Feedback.
     * Attaches admin feedback and resets item to PENDING_REVIEW
     * so the seller revises and resubmits.
     */
    Item sendPriceFeedback(Long itemId, String feedback);

    /** Admin moderation queue — items in PENDING_REVIEW, oldest first. */
    List<Item> getPendingItems();

    /** Admin items page — all APPROVED items (to send price feedback). */
    List<Item> getAllApprovedItems();

    /** Count items with a specific status (used for dashboard + report). */
    long countItemsByStatus(ItemStatus status);

    // ══════════════════════════════════════════════════════════════════════
    // CATEGORY — Admin use cases
    // ══════════════════════════════════════════════════════════════════════

    /** Create a new category. Throws {@link IllegalArgumentException} if name taken. */
    Category createCategory(String name, String description);

    /** All categories — used to populate category dropdowns everywhere. */
    List<Category> getAllCategories();

    /** Find a single category by PK. */
    Optional<Category> findCategoryById(Long id);

    /** Delete a category by PK. */
    void deleteCategory(Long id);

    // ══════════════════════════════════════════════════════════════════════
    // REPORTS — Admin use cases
    // ══════════════════════════════════════════════════════════════════════

    /**
     * UC8 Generate Reports.
     * Aggregates all statistics into a single DTO for the admin report page.
     */
    ReportDto generateReport();

    /** Total confirmed orders count (dashboard widget). */
    long countOrders();
}
