package com.oeurn_nurak.marketplace.dto.request;

import com.oeurn_nurak.marketplace.entity.enums.EmploymentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEmploymentStatusRequest(
        @NotNull(message = "Status is required") EmploymentStatus status
) {}
