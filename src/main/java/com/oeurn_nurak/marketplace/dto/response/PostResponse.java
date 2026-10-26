package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.ListingPost;
import java.util.List;
import java.time.LocalDateTime;

public record PostResponse(
        ListingPost post,
        List<ProductResponse> products,
        long reactionCount,
        long commentCount,
        boolean hasReacted,
        LocalDateTime createdAt,
        List<ProductEngagementResponse> productEngagements
) {}
