package com.pes.marketplace.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO for listing or updating an item.
 *
 * SOLID – SRP   : Only carries item form data; no persistence logic.
 * GRASP – Low Coupling : Decouples web layer from the Item entity.
 */
public class ItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 200)
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 2000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be > 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @NotNull(message = "Please select a category")
    private Long categoryId;

    // Getters & Setters
    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }
    public String getDescription()                  { return description; }
    public void setDescription(String d)            { this.description = d; }
    public BigDecimal getPrice()                    { return price; }
    public void setPrice(BigDecimal price)          { this.price = price; }
    public Long getCategoryId()                     { return categoryId; }
    public void setCategoryId(Long categoryId)      { this.categoryId = categoryId; }
}
