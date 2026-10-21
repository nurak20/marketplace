package com.oeurn_nurak.marketplace.repository;
import com.oeurn_nurak.marketplace.entity.ListingPostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ListingPostItemRepository extends JpaRepository<ListingPostItem, Long> {
    List<ListingPostItem> findByPostIdOrderByIdAsc(Long postId);
    boolean existsByPostIdAndProductId(Long postId, Long productId);
    void deleteByPostId(Long postId);
}
