package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateActiveRequest(@NotNull Boolean active) {}
