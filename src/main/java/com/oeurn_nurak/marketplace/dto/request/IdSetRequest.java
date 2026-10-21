package com.oeurn_nurak.marketplace.dto.request;
import jakarta.validation.constraints.*;
import java.util.Set;
public record IdSetRequest(@NotNull Set<@NotNull @Positive Long> ids) {}
