package com.pes.marketplace.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * Item category managed by Admins.
 *
 * SOLID – SRP : Only represents a classification group; no business logic.
 * GRASP – Information Expert : Category knows its name and which items belong to it.
 * GRASP – High Cohesion      : All fields relate to classification.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Item> items = new ArrayList<>();

    public Category() {}

    public Category(String name, String description) {
        this.name        = name;
        this.description = description;
    }

    // ── Getters & Setters ───────────────────────────────────────────────────
    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public String getName()                      { return name; }
    public void setName(String name)             { this.name = name; }

    public String getDescription()               { return description; }
    public void setDescription(String d)         { this.description = d; }

    public List<Item> getItems()                 { return items; }
    public void setItems(List<Item> items)       { this.items = items; }
}
