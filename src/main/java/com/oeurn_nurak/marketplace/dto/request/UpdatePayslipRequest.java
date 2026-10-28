package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdatePayslipRequest(
        @DecimalMin(value = "0.0", message = "Bonus cannot be negative")
        @Digits(integer = 10, fraction = 2) BigDecimal bonus,
        @DecimalMin(value = "0.0", message = "Overtime cannot be negative")
        @Digits(integer = 10, fraction = 2) BigDecimal overtime,
        @DecimalMin(value = "0.0", message = "Tax cannot be negative")
        @Digits(integer = 10, fraction = 2) BigDecimal tax,
        @DecimalMin(value = "0.0", message = "Other deductions cannot be negative")
        @Digits(integer = 10, fraction = 2) BigDecimal otherDeductions,
        @Size(max = 500, message = "Notes must be at most 500 characters") String notes
) {}
