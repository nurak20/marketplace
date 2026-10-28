package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EnrollEmployeeRequest(
        @NotNull(message = "User is required") Long userId,
        @Size(max = 30, message = "Employee code must be at most 30 characters") String employeeCode,
        @Size(max = 100, message = "Job title must be at most 100 characters") String jobTitle,
        @Size(max = 100, message = "Department must be at most 100 characters") String department,
        @NotNull(message = "Base salary is required")
        @DecimalMin(value = "0.0", message = "Base salary cannot be negative")
        @Digits(integer = 10, fraction = 2) BigDecimal baseSalary,
        @DecimalMin(value = "0.0", message = "Allowance cannot be negative")
        @Digits(integer = 10, fraction = 2) BigDecimal allowance,
        @Pattern(regexp = "[A-Za-z]{3}", message = "Currency must be a 3-letter code") String currency,
        LocalDate hireDate,
        @Size(max = 100, message = "Bank name must be at most 100 characters") String bankName,
        @Size(max = 50, message = "Bank account must be at most 50 characters") String bankAccount
) {}
