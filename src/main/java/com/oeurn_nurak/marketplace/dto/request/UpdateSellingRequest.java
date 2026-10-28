package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateSellingRequest(@NotNull Boolean canSell) {}
