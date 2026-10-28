package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.Notification;
import com.oeurn_nurak.marketplace.entity.User;
import com.oeurn_nurak.marketplace.entity.enums.NotifType;

import java.time.LocalDateTime;

/** Notification payload with enough context for the realtime list item. */
public record NotificationResponse(
        Long id,
        NotifType type,
        String message,
        Long refId,
        boolean isRead,
        LocalDateTime createdAt,
        int itemCount,
        Long postId,
        Long productId,
        UserResponse actor
) {
    public static NotificationResponse from(Notification notification, User actor) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getRefId(),
                Boolean.TRUE.equals(notification.getIsRead()),
                notification.getCreatedAt(),
                notification.getItemCount() == null ? 0 : notification.getItemCount(),
                notification.getPostId(),
                notification.getProductId(),
                actor == null ? null : UserResponse.from(actor));
    }
}
