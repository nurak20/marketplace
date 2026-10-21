package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
           SELECT m FROM Message m
           WHERE (m.senderId = :u1 AND m.receiverId = :u2)
              OR (m.senderId = :u2 AND m.receiverId = :u1)
           ORDER BY m.createdAt ASC
           """)
    List<Message> findConversation(@Param("u1") Long u1, @Param("u2") Long u2);

    @Query("SELECT m FROM Message m WHERE (m.senderId = :u1 AND m.receiverId = :u2) OR (m.senderId = :u2 AND m.receiverId = :u1)")
    Page<Message> findConversationPage(@Param("u1") Long u1, @Param("u2") Long u2, Pageable pageable);

    @Query(value = """
           SELECT DISTINCT ON (LEAST(m.sender_id, m.receiver_id), GREATEST(m.sender_id, m.receiver_id))
                  m.*
           FROM messages m
           WHERE m.sender_id = :userId OR m.receiver_id = :userId
           ORDER BY LEAST(m.sender_id, m.receiver_id),
                    GREATEST(m.sender_id, m.receiver_id),
                    m.created_at DESC
           """, nativeQuery = true)
    List<Message> findInbox(@Param("userId") Long userId);

    List<Message> findBySenderIdAndReceiverIdAndIsReadFalse(Long senderId, Long receiverId);
}
