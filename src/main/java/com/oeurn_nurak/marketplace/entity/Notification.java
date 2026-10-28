package com.oeurn_nurak.marketplace.entity;

import com.oeurn_nurak.marketplace.entity.enums.NotifType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notif_type")
    private NotifType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount = 0;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
