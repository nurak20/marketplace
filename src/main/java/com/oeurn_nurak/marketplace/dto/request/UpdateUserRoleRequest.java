package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRoleRequest(@NotBlank @Size(max = 50) String roleName) {}
