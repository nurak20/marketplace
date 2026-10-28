package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePayrollRunRequest(
        @NotNull(message = "Year is required")
        @Min(value = 2000, message = "Year must be 2000 or later")
        @Max(value = 2100, message = "Year must be 2100 or earlier") Integer year,
        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12") Integer month,
        @Pattern(regexp = "[A-Za-z]{3}", message = "Currency must be a 3-letter code") String currency,
        @Size(max = 500, message = "Notes must be at most 500 characters") String notes
) {}
