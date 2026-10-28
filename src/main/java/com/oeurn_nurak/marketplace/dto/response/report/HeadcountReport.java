package com.oeurn_nurak.marketplace.dto.response.report;

public record HeadcountReport(
        String department,
        long active,
        long onLeave,
        long terminated,
        long total
) {
}
