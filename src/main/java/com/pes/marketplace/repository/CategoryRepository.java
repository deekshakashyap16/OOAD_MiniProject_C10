package com.pes.marketplace.repository;

import com.pes.marketplace.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data-access abstraction for Category.
 *
 * SOLID – DIP : Services depend on this interface, not on Hibernate.
 * GRASP – Low Coupling : Only CategoryService (via MarketplaceService) calls this.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);
}
