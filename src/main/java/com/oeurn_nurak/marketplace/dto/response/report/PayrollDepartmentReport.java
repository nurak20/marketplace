package com.oeurn_nurak.marketplace.dto.response.report;

import java.math.BigDecimal;

public record PayrollDepartmentReport(
        int periodYear,
        int periodMonth,
        String department,
        long employees,
        BigDecimal gross,
        BigDecimal deductions,
        BigDecimal net
) {
}
