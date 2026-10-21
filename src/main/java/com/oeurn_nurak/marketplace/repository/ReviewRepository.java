package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductId(Long productId, Pageable pageable);
    boolean existsByProductIdAndBuyerId(Long productId, Long buyerId);
    Optional<Review> findByProductIdAndBuyerId(Long productId, Long buyerId);
    Page<Review> findBySellerId(Long sellerId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.sellerId = :sellerId")
    Double findAvgRatingBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.sellerId = :sellerId")
    Long countBySellerId(@Param("sellerId") Long sellerId);
}
