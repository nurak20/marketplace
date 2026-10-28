package com.oeurn_nurak.marketplace.dto.response;

import java.util.List;
import java.time.LocalDateTime;

public record PostResponse(
        PostSummaryResponse post,
        List<ProductResponse> products,
        long reactionCount,
        long commentCount,
        boolean hasReacted,
        LocalDateTime createdAt,
        List<ProductEngagementResponse> productEngagements,
        UserResponse postByUser
) {}
