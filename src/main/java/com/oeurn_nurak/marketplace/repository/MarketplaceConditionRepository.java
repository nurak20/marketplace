package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.MarketplaceCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketplaceConditionRepository extends JpaRepository<MarketplaceCondition, String> {
    List<MarketplaceCondition> findAllByOrderByCodeAsc();
    List<MarketplaceCondition> findByActiveTrueOrderByCodeAsc();
}
