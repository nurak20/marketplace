package com.oeurn_nurak.marketplace.entity;

import com.oeurn_nurak.marketplace.entity.enums.PayrollRunStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payroll_run_status")
    private PayrollRunStatus status = PayrollRunStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "total_gross", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_net", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
