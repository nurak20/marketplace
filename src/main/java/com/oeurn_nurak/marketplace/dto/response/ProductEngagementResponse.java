package com.oeurn_nurak.marketplace.dto.response;

public record ProductEngagementResponse(
        Long productId,
        long reactionCount,
        long commentCount,
        boolean hasReacted
) {}
