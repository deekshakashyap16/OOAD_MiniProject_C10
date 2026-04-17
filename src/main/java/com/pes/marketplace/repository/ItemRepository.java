package com.pes.marketplace.repository;

import com.pes.marketplace.model.Category;
import com.pes.marketplace.model.Item;
import com.pes.marketplace.model.ItemStatus;
import com.pes.marketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Persistence layer for {@link Item}.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * DIP  : MarketplaceService depends on this interface, not Hibernate directly.
 * ISP  : Only item-related queries — order/user queries are in separate repos.
 * SRP  : Sole responsibility is item data access.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Pure Fabrication : Exists purely for persistence, not a real-world concept.
 * Information Expert : Holds item-specific query logic (search, filter, count).
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /** UC4 Moderate Listings — fetch all items with a given status. */
    List<Item> findByStatus(ItemStatus status);

    /** UC5 Update Item / Seller Dashboard — all items listed by one seller. */
    List<Item> findBySeller(User seller);

    /** UC4 — pending items ordered oldest-first for fair review queue. */
    List<Item> findByStatusOrderByCreatedAtAsc(ItemStatus status);

    /** UC8 Generate Reports — count items per status. */
    long countByStatus(ItemStatus status);

    /**
     * UC2 Search & Browse Items.
     *
     * Returns only APPROVED items. Every filter is optional:
     * passing null for keyword / categoryId / minPrice / maxPrice skips that filter.
     *
     * ── GRASP Information Expert ────────────────────────────────────────────
     * This query encapsulates "what makes an item visible to a buyer" in one place.
     */
    @Query("""
            SELECT i FROM Item i
            WHERE i.status = 'APPROVED'
              AND (:keyword   IS NULL
                   OR LOWER(i.name)        LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR i.category.id = :categoryId)
              AND (:minPrice   IS NULL OR i.price >= :minPrice)
              AND (:maxPrice   IS NULL OR i.price <= :maxPrice)
            ORDER BY i.createdAt DESC
            """)
    List<Item> searchItems(
            @Param("keyword")    String     keyword,
            @Param("categoryId") Long       categoryId,
            @Param("minPrice")   BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice
    );
}
