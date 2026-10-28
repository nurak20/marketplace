package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.ListingPost;

import java.time.LocalDateTime;

public record PostSummaryResponse(Long id, Long userId, String content,
                                  LocalDateTime createdAt, boolean removed) {
    public static PostSummaryResponse from(ListingPost post) {
        boolean removed = post.getDeletedAt() != null;
        return new PostSummaryResponse(post.getId(), post.getUserId(),
                removed ? "This listing is no longer available." : post.getContent(),
                post.getCreatedAt(), removed);
    }
}
