package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.ListingPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingPostRepository extends JpaRepository<ListingPost, Long> {
    Page<ListingPost> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
