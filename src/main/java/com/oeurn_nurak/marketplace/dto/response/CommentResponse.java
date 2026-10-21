package com.oeurn_nurak.marketplace.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        Long postId,
        Long productId,
        Long userId,
        Long parentId,
        String content,
        LocalDateTime createdAt,
        String username,
        String fullName,
        String avatarUrl,
        List<CommentResponse> replies) {
}
