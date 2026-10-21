package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {
    private String content;

    @NotEmpty(message = "At least one product ID is required")
    private List<@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long> productIds;
}
