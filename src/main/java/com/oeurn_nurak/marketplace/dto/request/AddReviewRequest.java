package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddReviewRequest {
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Short rating;

    private String comment;
}
