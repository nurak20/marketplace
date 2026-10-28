package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.EmployeeProfile;
import com.oeurn_nurak.marketplace.entity.User;
import com.oeurn_nurak.marketplace.entity.enums.EmploymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeProfileResponse(Long id, Long userId, String username, String fullName,
                                      String employeeCode, String jobTitle, String department,
                                      BigDecimal baseSalary, BigDecimal allowance, String currency,
                                      LocalDate hireDate, String bankName, String bankAccount,
                                      EmploymentStatus status, LocalDateTime createdAt,
                                      LocalDateTime updatedAt) {
    public static EmployeeProfileResponse from(EmployeeProfile profile, User user) {
        return new EmployeeProfileResponse(profile.getId(), profile.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getFullName(),
                profile.getEmployeeCode(), profile.getJobTitle(), profile.getDepartment(),
                profile.getBaseSalary(), profile.getAllowance(), profile.getCurrency(),
                profile.getHireDate(), profile.getBankName(), profile.getBankAccount(),
                profile.getStatus(), profile.getCreatedAt(), profile.getUpdatedAt());
    }
}
