package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarketplaceCategoryRequest(
        @NotBlank @Size(max = 100) String name
) {}
