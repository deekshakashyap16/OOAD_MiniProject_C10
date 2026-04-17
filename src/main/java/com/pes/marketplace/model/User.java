package com.pes.marketplace.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents any campus user: Buyer, Seller, or Admin.
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only models user data; no business logic lives here.
 * OCP : New roles can be added to the Role enum without modifying this class.
 * LSP : N/A (no inheritance hierarchy here; role-based polymorphism via Role enum).
 * ISP : Entity exposes only the fields services need.
 * DIP : No direct dependency on concrete services.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Information Expert : User knows its own role, email, and active status.
 * Low Coupling       : User holds only JPA references to related entities.
 * High Cohesion      : All fields are user-identity concerns.
 * Creator            : UserService creates User instances (not this class).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String name;

    @Email @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Item> listedItems = new ArrayList<>();

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // ── Constructors ────────────────────────────────────────────────────────
    public User() {}

    public User(String name, String email, String password, Role role) {
        this.name     = name;
        this.email    = email;
        this.password = password;
        this.role     = role;
    }

    // ── Getters & Setters ───────────────────────────────────────────────────
    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }

    public String getPassword()                 { return password; }
    public void setPassword(String password)    { this.password = password; }

    public Role getRole()                       { return role; }
    public void setRole(Role role)              { this.role = role; }

    public boolean isActive()                   { return active; }
    public void setActive(boolean active)       { this.active = active; }

    public LocalDateTime getCreatedAt()         { return createdAt; }

    public List<Item> getListedItems()          { return listedItems; }
    public void setListedItems(List<Item> l)    { this.listedItems = l; }

    public List<Order> getOrders()              { return orders; }
    public void setOrders(List<Order> o)        { this.orders = o; }
}
