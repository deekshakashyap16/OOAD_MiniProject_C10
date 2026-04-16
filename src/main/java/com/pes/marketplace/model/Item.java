package com.pes.marketplace.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a product listing on the marketplace.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only holds item data; state-transition logic is in ItemService.
 * OCP : New statuses can be added to ItemStatus without touching this class.
 * DIP : Depends on Category/User interfaces (JPA lazy references), not concretes.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Information Expert : Item knows whether it is available (APPROVED status).
 * Low Coupling       : Only JPA FK references to User and Category.
 * High Cohesion      : All fields describe a marketplace listing.
 * Protected Variation: isAvailable() hides the status-check rule behind a method.
 */
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(min = 2, max = 200)
    @Column(nullable = false)
    private String name;

    @NotBlank @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String description;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status = ItemStatus.PENDING_REVIEW;

    /** Admin feedback to seller (used for reject or price-modification feedback). */
    @Column(name = "admin_feedback", length = 1000)
    private String adminFeedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // ── GRASP: Information Expert – Item knows its availability ────────────
    public boolean isAvailable() { return this.status == ItemStatus.APPROVED; }

    // ── Constructors ────────────────────────────────────────────────────────
    public Item() {}

    public Item(String name, String description, BigDecimal price, User seller, Category category) {
        this.name        = name;
        this.description = description;
        this.price       = price;
        this.seller      = seller;
        this.category    = category;
    }

    // ── Getters & Setters ───────────────────────────────────────────────────
    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public String getName()                      { return name; }
    public void setName(String name)             { this.name = name; }

    public String getDescription()               { return description; }
    public void setDescription(String d)         { this.description = d; }

    public BigDecimal getPrice()                 { return price; }
    public void setPrice(BigDecimal price)       { this.price = price; }

    public ItemStatus getStatus()                { return status; }
    public void setStatus(ItemStatus status)     { this.status = status; }

    public String getAdminFeedback()             { return adminFeedback; }
    public void setAdminFeedback(String f)       { this.adminFeedback = f; }

    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }

    public User getSeller()                      { return seller; }
    public void setSeller(User seller)           { this.seller = seller; }

    public Category getCategory()                { return category; }
    public void setCategory(Category c)          { this.category = c; }
}
