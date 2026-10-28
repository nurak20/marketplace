package com.oeurn_nurak.marketplace.dto.response.report;

import java.math.BigDecimal;

public record ReportOverview(
        long totalUsers,
        long activeUsers,
        long sellers,
        long products,
        long availableProducts,
        long soldProducts,
        long posts,
        long comments,
        long reactions,
        long wishlists,
        long employees,
        long activeEmployees,
        BigDecimal paidGross,
        BigDecimal paidNet,
        BigDecimal availableValue
) {
}
