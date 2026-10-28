package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.Message;
import com.oeurn_nurak.marketplace.entity.User;

import java.time.LocalDateTime;

/** Conversation message with the participant profiles the client renders. */
public record MessageResponse(Long id, Long senderId, Long receiverId, Long productId,
                              String content, boolean isRead, LocalDateTime createdAt,
                              UserResponse sender, UserResponse receiver) {
    public static MessageResponse from(Message message, User sender, User receiver) {
        return new MessageResponse(
                message.getId(),
                message.getSenderId(),
                message.getReceiverId(),
                message.getProductId(),
                message.getContent(),
                Boolean.TRUE.equals(message.getIsRead()),
                message.getCreatedAt(),
                sender == null ? null : UserResponse.from(sender),
                receiver == null ? null : UserResponse.from(receiver));
    }
}
