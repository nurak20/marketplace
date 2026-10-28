package com.oeurn_nurak.marketplace.dto.response.report;

import java.math.BigDecimal;

public record ProductStatusReport(String status, long products, BigDecimal value) {
}
