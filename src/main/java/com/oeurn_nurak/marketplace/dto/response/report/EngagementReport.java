package com.oeurn_nurak.marketplace.dto.response.report;

public record EngagementReport(
        String month,
        long comments,
        long reactions,
        long wishlists,
        long reviews,
        long messages
) {
}
