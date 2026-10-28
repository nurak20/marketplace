package com.oeurn_nurak.marketplace.dto.response.report;

import java.math.BigDecimal;

public record TopSellerReport(
        long userId,
        String name,
        long listings,
        long sold,
        BigDecimal revenue,
        long reactions,
        long comments
) {
}
