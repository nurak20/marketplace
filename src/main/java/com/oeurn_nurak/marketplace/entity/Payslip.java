package com.oeurn_nurak.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payslips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "employee_code", nullable = false, length = 30)
    private String employeeCode;

    @Column(name = "employee_name", nullable = false, length = 100)
    private String employeeName;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(length = 100)
    private String department;

    @Builder.Default
    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal allowance = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal overtime = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "other_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "gross_pay", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossPay = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "deductions_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal deductionsTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "net_pay", nullable = false, precision = 12, scale = 2)
    private BigDecimal netPay = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
