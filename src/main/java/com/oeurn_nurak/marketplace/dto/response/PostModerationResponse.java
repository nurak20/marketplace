package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.ListingPost;

import java.time.LocalDateTime;

public record PostModerationResponse(Long id, Long userId, String content,
                                     LocalDateTime createdAt, boolean removed) {
    public static PostModerationResponse from(ListingPost post) {
        return new PostModerationResponse(post.getId(), post.getUserId(), post.getContent(),
                post.getCreatedAt(), post.getDeletedAt() != null);
    }
}
