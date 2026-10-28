package com.oeurn_nurak.marketplace.dto.response.report;

public record MonthlyActivityReport(
        String month,
        long users,
        long products,
        long posts,
        long comments,
        long reactions
) {
}
