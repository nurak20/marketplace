package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    Optional<PostReaction> findByPostIdAndProductIdIsNullAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndProductIdIsNullAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);

    Optional<PostReaction> findByPostIdAndProductIdAndUserId(Long postId, Long productId, Long userId);
    boolean existsByPostIdAndProductIdAndUserId(Long postId, Long productId, Long userId);
    long countByPostIdAndProductId(Long postId, Long productId);
}
