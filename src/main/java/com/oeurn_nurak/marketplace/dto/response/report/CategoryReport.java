package com.oeurn_nurak.marketplace.dto.response.report;

import java.math.BigDecimal;

public record CategoryReport(
        String category,
        long products,
        long available,
        long sold,
        long reserved,
        BigDecimal value
) {
}
