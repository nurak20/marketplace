package com.oeurn_nurak.marketplace.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record MarketplacePageResponse<T>(List<T> items, int page, int size,
                                         long totalElements, int totalPages,
                                         List<String> categories, List<String> conditions) {
    public static <T> MarketplacePageResponse<T> from(Page<T> page,
                                                       List<String> categories,
                                                       List<String> conditions) {
        return new MarketplacePageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), categories, conditions);
    }
}
