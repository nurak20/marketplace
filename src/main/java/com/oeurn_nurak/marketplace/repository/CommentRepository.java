package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPostIdAndProductIdIsNullAndParentIdIsNullOrderByCreatedAtAsc(Long postId, Pageable pageable);
    java.util.List<Comment> findByPostIdAndProductIdIsNullOrderByCreatedAtAsc(Long postId);
    long countByPostId(Long postId);
    long countByPostIdAndProductIdIsNull(Long postId);

    Page<Comment> findByPostIdAndProductIdAndParentIdIsNullOrderByCreatedAtAsc(Long postId, Long productId, Pageable pageable);
    java.util.List<Comment> findByPostIdAndProductIdOrderByCreatedAtAsc(Long postId, Long productId);
    long countByPostIdAndProductId(Long postId, Long productId);
}
