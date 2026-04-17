package com.pes.marketplace.repository;

import com.pes.marketplace.model.Order;
import com.pes.marketplace.model.OrderStatus;
import com.pes.marketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Persistence layer for {@link Order}.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * DIP  : MarketplaceService / OrderService depends on this interface.
 * ISP  : Only order-related queries live here.
 * SRP  : Sole responsibility is order data access.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Pure Fabrication : Exists purely for persistence concerns.
 * Information Expert : Owns all "what do I know about orders?" query logic.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * UC6 View Purchase History — all orders for one buyer, newest first.
     */
    List<Order> findByBuyerOrderByOrderDateDesc(User buyer);

    /**
     * UC8 Generate Reports — count orders by status.
     */
    long countByStatus(OrderStatus status);

    /**
     * UC8 Generate Reports — total revenue from all CONFIRMED orders.
     * COALESCE guards against null when no orders exist yet.
     */
    @Query("SELECT COALESCE(SUM(o.purchasePrice), 0) FROM Order o WHERE o.status = 'CONFIRMED'")
    BigDecimal sumRevenue();
}
