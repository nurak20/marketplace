package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.ListingPost;
import com.oeurn_nurak.marketplace.entity.Product;
import java.util.List;
import java.time.LocalDateTime;

public record PostResponse(
        ListingPost post,
        List<Product> products,
        long reactionCount,
        long commentCount,
        boolean hasReacted,
        LocalDateTime createdAt,
        List<ProductEngagementResponse> productEngagements
) {}
