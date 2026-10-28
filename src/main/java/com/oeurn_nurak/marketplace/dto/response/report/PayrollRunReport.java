package com.oeurn_nurak.marketplace.dto.response.report;

import java.math.BigDecimal;

public record PayrollRunReport(
        int periodYear,
        int periodMonth,
        String status,
        long employees,
        BigDecimal gross,
        BigDecimal deductions,
        BigDecimal net
) {
}
