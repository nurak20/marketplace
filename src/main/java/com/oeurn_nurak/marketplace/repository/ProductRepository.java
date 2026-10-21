package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p WHERE p.id IN (SELECT w.productId FROM Wishlist w WHERE w.userId = :userId)")
    Page<Product> findWishlisted(@Param("userId") Long userId, Pageable pageable);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.deleted_at IS NULL
              AND (:keyword IS NULL OR p.search_vector @@ plainto_tsquery('english', :keyword))
            """, nativeQuery = true)
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
