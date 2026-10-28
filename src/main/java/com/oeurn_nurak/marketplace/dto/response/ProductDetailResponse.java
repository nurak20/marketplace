package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.Review;
import java.util.List;

public record ProductDetailResponse(
        ProductResponse product,
        long totalLike,
        long totalComment,
        boolean hasLiked,
        UserResponse postByUser,
        List<CommentResponse> comments,
        List<Review> reviews
) {}
