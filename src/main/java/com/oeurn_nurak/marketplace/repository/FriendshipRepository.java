package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.Friendship;
import com.oeurn_nurak.marketplace.entity.enums.FriendStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("""
           SELECT f FROM Friendship f
           WHERE (f.requesterId = :userId OR f.addresseeId = :userId)
             AND f.status = :status
           """)
    Page<Friendship> findByUserIdAndStatus(@Param("userId") Long userId,
                                           @Param("status") FriendStatus status,
                                           Pageable pageable);

    @Query("""
           SELECT f FROM Friendship f
           WHERE f.addresseeId = :userId AND f.status = 'PENDING'
           """)
    Page<Friendship> findPendingRequestsForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("""
           SELECT f FROM Friendship f
           WHERE (f.requesterId = :u1 AND f.addresseeId = :u2)
              OR (f.requesterId = :u2 AND f.addresseeId = :u1)
           """)
    Optional<Friendship> findBetweenUsers(@Param("u1") Long u1, @Param("u2") Long u2);
}
