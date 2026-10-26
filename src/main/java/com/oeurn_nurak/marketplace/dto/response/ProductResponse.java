package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.Product;
import com.oeurn_nurak.marketplace.entity.enums.ConditionType;
import com.oeurn_nurak.marketplace.entity.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        Long sellerId,
        String title,
        String description,
        BigDecimal price,
        String category,
        ConditionType condition,
        String location,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        String imageUrl,
        UserResponse seller,
        boolean isSaved
) {
    public static ProductResponse from(
            Product product,
            String imageUrl,
            UserResponse seller,
            boolean isSaved
    ) {
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getCondition(),
                product.getLocation(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getDeletedAt(),
                imageUrl,
                seller,
                isSaved
        );
    }
}
