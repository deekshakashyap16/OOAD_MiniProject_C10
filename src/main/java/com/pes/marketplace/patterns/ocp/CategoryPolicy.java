package com.pes.marketplace.patterns.ocp;

/*
 * Open/Closed Principle
 *     - Software entities should be open for extension but
 *       closed for modification.
 *
 * Using inheritance and abstraction, new category types can be added
 * without modifying existing code.  ListingFeeCalculator (same package)
 * depends only on this abstraction, so introducing a new category
 * requires zero edits to existing policies or callers.
 */
abstract class CategoryPolicy {
    abstract double calculateListingFee();
}
