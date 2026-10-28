package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.MarketplaceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceCategoryRepository extends JpaRepository<MarketplaceCategory, Long> {
    List<MarketplaceCategory> findAllByOrderByNameAsc();
    List<MarketplaceCategory> findByActiveTrueOrderByNameAsc();
    Optional<MarketplaceCategory> findByNameIgnoreCase(String name);
}
