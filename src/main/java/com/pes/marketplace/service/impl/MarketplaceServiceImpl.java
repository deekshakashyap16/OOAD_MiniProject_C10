package com.pes.marketplace.service.impl;

import com.pes.marketplace.dto.ItemRequest;
import com.pes.marketplace.dto.ReportDto;
import com.pes.marketplace.exception.*;
import com.pes.marketplace.model.*;
import com.pes.marketplace.patterns.ocp.ListingFeeCalculator;
import com.pes.marketplace.repository.*;
import com.pes.marketplace.service.MarketplaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Concrete implementation of {@link MarketplaceService}.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only marketplace business logic — auth/user work stays in AuthServiceImpl.
 * OCP : New features (auction, wishlist) extend this class or its interface
 *       without modifying existing methods.
 * LSP : Fully substitutable for MarketplaceService wherever injected.
 * DIP : Depends only on repository interfaces, not concrete Hibernate classes.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Creator          : Creates Item objects (has all needed data: request + seller).
 *                    Creates Order objects (has buyer + item at call time).
 * Information Expert : Holds all item/order/category business rules.
 * Controller (GRASP): Coordinates use cases by delegating to repositories.
 * Protected Variation: State-transition checks (PENDING_REVIEW → APPROVED) are
 *                      hidden here so callers never encode status logic themselves.
 * Low Coupling     : No controller or view logic bleeds into this class.
 * High Cohesion    : Every method is a marketplace-domain operation.
 */
@Service
@Transactional
public class MarketplaceServiceImpl implements MarketplaceService {

    private final ItemRepository       itemRepository;
    private final OrderRepository      orderRepository;
    private final CategoryRepository   categoryRepository;
    private final UserRepository       userRepository;   // needed for report counts only
    private final ListingFeeCalculator listingFeeCalculator; // OCP: category-policy entry point

    /** Constructor injection — enables easy unit-testing with mocks. */
    public MarketplaceServiceImpl(ItemRepository itemRepository,
                                  OrderRepository orderRepository,
                                  CategoryRepository categoryRepository,
                                  UserRepository userRepository,
                                  ListingFeeCalculator listingFeeCalculator) {
        this.itemRepository       = itemRepository;
        this.orderRepository      = orderRepository;
        this.categoryRepository   = categoryRepository;
        this.userRepository       = userRepository;
        this.listingFeeCalculator = listingFeeCalculator;
    }

    // ══════════════════════════════════════════════════════════════════════
    // UC1 — List Item for Sale (Seller / Major)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * GRASP Creator : MarketplaceServiceImpl creates Item because it holds
     *                 ItemRequest (all initialisation data) and the seller.
     * GRASP Protected Variation : Initial status is always PENDING_REVIEW —
     *                             the rule is enforced once, here.
     */
    @Override
    public Item listItem(ItemRequest request, User seller) {
        Category category = resolveCategory(request.getCategoryId());

        // OCP in action: the fee for this listing is derived by the
        // CategoryPolicy hierarchy — no switch/if-ladder lives here.
        double listingFee = listingFeeCalculator.calculateFee(category);
        System.out.printf("[OCP] Listing fee for '%s' category: ₹%.2f%n",
                category.getName(), listingFee);

        Item item = new Item(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                seller,
                category
        );
        // PENDING_REVIEW is the default set in Item's field initialiser;
        // we set it explicitly here for clarity and documentation.
        item.setStatus(ItemStatus.PENDING_REVIEW);
        return itemRepository.save(item);
    }

    // ── OCP: Listing fee exposure ───────────────────────────────────────────

    /**
     * Delegates to ListingFeeCalculator — the only place where the
     * CategoryPolicy hierarchy is consumed.  Controllers use this to
     * display fees without ever touching a concrete policy class.
     */
    @Override
    @Transactional(readOnly = true)
    public double calculateListingFee(Category category) {
        return listingFeeCalculator.calculateFee(category);
    }

    // ══════════════════════════════════════════════════════════════════════
    // UC5 — Update Item Details (Seller / Minor)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * SOLID SRP           : Authorisation check + update logic are both "item update" concerns.
     * GRASP Info Expert   : Item knows its own seller and status.
     * GRASP Protected Var : After any seller edit the item resets to PENDING_REVIEW
     *                       so the admin re-approves — rule is hidden here, not in controller.
     */
    @Override
    public Item updateItem(Long itemId, ItemRequest request, User seller) {
        Item item = resolveItem(itemId);

        if (!item.getSeller().getId().equals(seller.getId())) {
            throw new UnauthorizedActionException("You may only edit your own listings.");
        }
        if (item.getStatus() == ItemStatus.SOLD || item.getStatus() == ItemStatus.REMOVED) {
            throw new IllegalStateException("Sold or removed items cannot be edited.");
        }

        Category category = resolveCategory(request.getCategoryId());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(category);
        item.setAdminFeedback(null);          // clear previous feedback
        item.setStatus(ItemStatus.PENDING_REVIEW); // reset for re-approval
        return itemRepository.save(item);
    }

    // ── UC5 Remove Listing ─────────────────────────────────────────────────

    @Override
    public void removeItem(Long itemId, User seller) {
        Item item = resolveItem(itemId);
        if (!item.getSeller().getId().equals(seller.getId())) {
            throw new UnauthorizedActionException("You may only remove your own listings.");
        }
        if (item.getStatus() == ItemStatus.SOLD) {
            throw new IllegalStateException("A sold item cannot be removed.");
        }
        item.setStatus(ItemStatus.REMOVED);
        itemRepository.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> getItemsBySeller(User seller) {
        return itemRepository.findBySeller(seller);
    }

    // ══════════════════════════════════════════════════════════════════════
    // UC2 — Search & Browse Items (Buyer / Major)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * GRASP Information Expert : ItemRepository owns the JPQL search logic;
     *                            this method just sanitises the keyword and delegates.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Item> searchItems(String keyword, Long categoryId,
                                  BigDecimal minPrice, BigDecimal maxPrice) {
        // Treat blank string the same as null so the JPQL IS NULL check fires correctly
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        return itemRepository.searchItems(kw, categoryId, minPrice, maxPrice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> getApprovedItems() {
        return itemRepository.findByStatus(ItemStatus.APPROVED);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Item> findItemById(Long id) {
        return itemRepository.findById(id);
    }

    // ══════════════════════════════════════════════════════════════════════
    // UC3 — Purchase Item (Buyer / Major)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Template Method pattern:
     *   Step 1 – Verify item exists.
     *   Step 2 – Check availability (GRASP Info Expert: item.isAvailable()).
     *   Step 3 – Prevent self-purchase.
     *   Step 4 – Create Order (GRASP Creator: service has buyer + item).
     *   Step 5 – Mark item SOLD atomically (@Transactional).
     *   Step 6 – Persist and return.
     *
     * GRASP Protected Variation : All purchase-integrity rules are in one place.
     */
    @Override
    public Order purchaseItem(Long itemId, User buyer) {
        // Step 1
        Item item = resolveItem(itemId);

        // Step 2 — GRASP Info Expert: Item knows whether it is available
        if (!item.isAvailable()) {
            throw new ItemNotAvailableException(
                    "'" + item.getName() + "' is not available. Current status: " + item.getStatus());
        }

        // Step 3
        if (item.getSeller().getId().equals(buyer.getId())) {
            throw new UnauthorizedActionException("You cannot purchase your own listing.");
        }

        // Step 4 — GRASP Creator: Order(buyer, item) — service has both
        Order order = new Order(buyer, item);

        // Step 5 — mark sold BEFORE saving order to prevent race conditions
        item.setStatus(ItemStatus.SOLD);
        itemRepository.save(item);

        // Step 6
        return orderRepository.save(order);
    }

    // ══════════════════════════════════════════════════════════════════════
    // UC6 — View Purchase History (Buyer / Minor)
    // ══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<Order> getPurchaseHistory(User buyer) {
        return orderRepository.findByBuyerOrderByOrderDateDesc(buyer);
    }

    // ══════════════════════════════════════════════════════════════════════
    // UC4 — Moderate Listings (Admin / Major)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * GRASP Protected Variation : Only PENDING_REVIEW items can be moderated;
     *                             only APPROVED / REJECTED are valid target states.
     *                             Controllers cannot bypass this — the rule lives here.
     */
    @Override
    public Item moderateItem(Long itemId, ItemStatus newStatus, String feedback) {
        Item item = resolveItem(itemId);

        if (item.getStatus() != ItemStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Only items in PENDING_REVIEW can be moderated. Current: " + item.getStatus());
        }
        if (newStatus != ItemStatus.APPROVED && newStatus != ItemStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "Moderation target must be APPROVED or REJECTED, got: " + newStatus);
        }

        item.setStatus(newStatus);
        if (feedback != null && !feedback.isBlank()) {
            item.setAdminFeedback(feedback);
        }
        return itemRepository.save(item);
    }

    // ══════════════════════════════════════════════════════════════════════
    // UC7 — Send Price Modification Feedback (Admin / Minor)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attaches admin feedback and resets to PENDING_REVIEW so the seller
     * must revise price and resubmit before the item goes live again.
     */
    @Override
    public Item sendPriceFeedback(Long itemId, String feedback) {
        Item item = resolveItem(itemId);
        item.setAdminFeedback(feedback);
        item.setStatus(ItemStatus.PENDING_REVIEW);
        return itemRepository.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> getPendingItems() {
        return itemRepository.findByStatusOrderByCreatedAtAsc(ItemStatus.PENDING_REVIEW);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> getAllApprovedItems() {
        return itemRepository.findByStatus(ItemStatus.APPROVED);
    }

    @Override
    @Transactional(readOnly = true)
    public long countItemsByStatus(ItemStatus status) {
        return itemRepository.countByStatus(status);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Category management (Admin)
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public Category createCategory(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Category already exists: " + name);
        }
        return categoryRepository.save(new Category(name, description));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public void deleteCategory(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        categoryRepository.delete(cat);
    }

    // ══════════════════════════════════════════════════════════════════════
    // UC8 — Generate Reports (Admin / Minor)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Assembles all statistics into a ReportDto in one call.
     * GRASP Information Expert : Each repository is the expert for its own counts.
     */
    @Override
    @Transactional(readOnly = true)
    public ReportDto generateReport() {
        ReportDto r = new ReportDto();
        r.setTotalUsers(userRepository.countActiveUsers());
        r.setTotalBuyers(userRepository.countActiveByRole(Role.BUYER));
        r.setTotalSellers(userRepository.countActiveByRole(Role.SELLER));
        r.setPendingListings(itemRepository.countByStatus(ItemStatus.PENDING_REVIEW));
        r.setApprovedListings(itemRepository.countByStatus(ItemStatus.APPROVED));
        r.setRejectedListings(itemRepository.countByStatus(ItemStatus.REJECTED));
        r.setSoldListings(itemRepository.countByStatus(ItemStatus.SOLD));
        r.setTotalOrders(orderRepository.countByStatus(OrderStatus.CONFIRMED)
                       + orderRepository.countByStatus(OrderStatus.COMPLETED));
        r.setTotalRevenue(orderRepository.sumRevenue());
        return r;
    }

    @Override
    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.countByStatus(OrderStatus.CONFIRMED)
             + orderRepository.countByStatus(OrderStatus.COMPLETED);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Private helpers — DRY + GRASP Information Expert
    // ══════════════════════════════════════════════════════════════════════

    private Item resolveItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));
    }

    private Category resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + categoryId));
    }
}
