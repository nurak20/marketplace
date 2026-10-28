package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.MarketplaceCategory;
import com.oeurn_nurak.marketplace.entity.MarketplaceCondition;

import java.util.List;

public record MarketplaceOptionsResponse(List<String> categories, List<String> conditions) {
    public static MarketplaceOptionsResponse from(List<MarketplaceCategory> categories,
                                                   List<MarketplaceCondition> conditions) {
        return new MarketplaceOptionsResponse(
                categories.stream().map(MarketplaceCategory::getName).toList(),
                conditions.stream().map(MarketplaceCondition::getCode).toList());
    }
}
