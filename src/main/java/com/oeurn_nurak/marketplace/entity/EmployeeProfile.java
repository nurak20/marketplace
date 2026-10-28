package com.oeurn_nurak.marketplace.entity;

import com.oeurn_nurak.marketplace.entity.enums.EmploymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "employee_code", nullable = false, unique = true, length = 30)
    private String employeeCode;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(length = 100)
    private String department;

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal allowance = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "employment_status")
    private EmploymentStatus status = EmploymentStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
