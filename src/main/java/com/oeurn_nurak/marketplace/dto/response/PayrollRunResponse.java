package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.PayrollRun;
import com.oeurn_nurak.marketplace.entity.enums.PayrollRunStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayrollRunResponse(Long id, Integer periodYear, Integer periodMonth, String currency,
                                 PayrollRunStatus status, String notes, BigDecimal totalGross,
                                 BigDecimal totalDeductions, BigDecimal totalNet, Long createdBy,
                                 Long approvedBy, LocalDateTime approvedAt, LocalDateTime paidAt,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static PayrollRunResponse from(PayrollRun run) {
        return new PayrollRunResponse(run.getId(), run.getPeriodYear(), run.getPeriodMonth(),
                run.getCurrency(), run.getStatus(), run.getNotes(), run.getTotalGross(),
                run.getTotalDeductions(), run.getTotalNet(), run.getCreatedBy(), run.getApprovedBy(),
                run.getApprovedAt(), run.getPaidAt(), run.getCreatedAt(), run.getUpdatedAt());
    }
}
