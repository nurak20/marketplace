package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.PayrollRun;
import com.oeurn_nurak.marketplace.entity.Payslip;
import com.oeurn_nurak.marketplace.entity.enums.PayrollRunStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayslipResponse(Long id, Long runId, Integer periodYear, Integer periodMonth,
                              PayrollRunStatus runStatus, Long userId, String employeeCode,
                              String employeeName, String jobTitle, String department,
                              BigDecimal baseSalary, BigDecimal allowance, BigDecimal bonus,
                              BigDecimal overtime, BigDecimal tax, BigDecimal otherDeductions,
                              BigDecimal grossPay, BigDecimal deductionsTotal, BigDecimal netPay,
                              String currency, String notes, LocalDateTime paidAt,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static PayslipResponse from(Payslip payslip, PayrollRun run) {
        return new PayslipResponse(payslip.getId(), payslip.getRunId(),
                run == null ? null : run.getPeriodYear(), run == null ? null : run.getPeriodMonth(),
                run == null ? null : run.getStatus(), payslip.getUserId(), payslip.getEmployeeCode(),
                payslip.getEmployeeName(), payslip.getJobTitle(), payslip.getDepartment(),
                payslip.getBaseSalary(), payslip.getAllowance(), payslip.getBonus(),
                payslip.getOvertime(), payslip.getTax(), payslip.getOtherDeductions(),
                payslip.getGrossPay(), payslip.getDeductionsTotal(), payslip.getNetPay(),
                payslip.getCurrency(), payslip.getNotes(), run == null ? null : run.getPaidAt(),
                payslip.getCreatedAt(), payslip.getUpdatedAt());
    }
}
