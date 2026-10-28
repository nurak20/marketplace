package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarketplaceConditionRequest(
        @NotBlank @Size(max = 30) String code
) {}
