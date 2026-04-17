package com.pes.marketplace.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a completed purchase transaction.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only stores transaction data; no business rules embedded here.
 * OCP : New order states can be added to OrderStatus without modifying Order.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Information Expert : Order knows its own status, price snapshot, and participants.
 * Creator            : OrderService creates Order objects (it has the needed info).
 * Low Coupling       : References User and Item by JPA FK only.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PLACED;

    /**
     * Price snapshot at purchase time.
     * Ensures purchase history is accurate even if seller later changes price.
     */
    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @PrePersist
    protected void onCreate() { this.orderDate = LocalDateTime.now(); }

    // ── Constructors ────────────────────────────────────────────────────────
    public Order() {}

    public Order(User buyer, Item item) {
        this.buyer         = buyer;
        this.item          = item;
        this.purchasePrice = item.getPrice();
        this.status        = OrderStatus.CONFIRMED;
    }

    // ── Getters & Setters ───────────────────────────────────────────────────
    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }

    public OrderStatus getStatus()                  { return status; }
    public void setStatus(OrderStatus status)       { this.status = status; }

    public BigDecimal getPurchasePrice()             { return purchasePrice; }
    public void setPurchasePrice(BigDecimal p)       { this.purchasePrice = p; }

    public LocalDateTime getOrderDate()              { return orderDate; }

    public User getBuyer()                           { return buyer; }
    public void setBuyer(User buyer)                 { this.buyer = buyer; }

    public Item getItem()                            { return item; }
    public void setItem(Item item)                   { this.item = item; }
}
