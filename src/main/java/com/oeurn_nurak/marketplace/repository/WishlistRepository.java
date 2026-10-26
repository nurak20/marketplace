package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);
    List<Wishlist> findByUserIdAndProductIdIn(Long userId, Collection<Long> productIds);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    Page<Wishlist> findByUserId(Long userId, Pageable pageable);
}
