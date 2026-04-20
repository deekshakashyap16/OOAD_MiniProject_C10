package com.pes.marketplace.patterns.ocp;

import com.pes.marketplace.model.Category;
import org.springframework.stereotype.Component;

/**
 * Public entry point for the OCP category-policy hierarchy.
 *
 * Callers (MarketplaceServiceImpl, SellerController) depend only on this
 * class — they never see the concrete policies.  Adding a new category
 * means creating a new CategoryPolicy subclass and adding one line to
 * resolvePolicy() here.  Existing policies are not touched.
 */
@Component
public class ListingFeeCalculator {

    public double calculateFee(Category category) {
        CategoryPolicy policy = resolvePolicy(category);
        return policy.calculateListingFee();
    }

    private CategoryPolicy resolvePolicy(Category category) {
        if (category == null || category.getName() == null) {
            return new DefaultPolicy();
        }
        String name = category.getName().toLowerCase();
        if (name.contains("electronic")) return new ElectronicsPolicy();
        if (name.contains("book"))       return new BooksPolicy();
        if (name.contains("cloth"))      return new ClothingPolicy();
        if (name.contains("station"))    return new StationeryPolicy();
        if (name.contains("sport"))      return new SportsFitnessPolicy();
        return new DefaultPolicy();
    }
}
