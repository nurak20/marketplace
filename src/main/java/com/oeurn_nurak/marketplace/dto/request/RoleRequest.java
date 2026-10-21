package com.oeurn_nurak.marketplace.dto.request;
import jakarta.validation.constraints.*;
public record RoleRequest(@NotBlank @Size(max = 50) @Pattern(regexp = "ROLE_[A-Z][A-Z0-9_]*") String name,
                          @Size(max = 200) String description) {}
