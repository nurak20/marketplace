package com.oeurn_nurak.marketplace.dto.request;
import com.oeurn_nurak.marketplace.entity.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;
public record UpdateStatusRequest(@NotNull ProductStatus status) {}
