package com.oeurn_nurak.marketplace.dto.request;
import jakarta.validation.constraints.*;
public record PermissionRequest(@NotBlank @Size(max = 100) @Pattern(regexp = "[a-z][a-z0-9_-]*:[a-z][a-z0-9_-]*") String name,
                                @Size(max = 200) String description) {}
