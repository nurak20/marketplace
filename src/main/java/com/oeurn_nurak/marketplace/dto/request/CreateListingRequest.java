package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CreateListingRequest(
        @Size(max = 2000) String content,
        @NotEmpty List<@Valid ProductDraft> products
) {
    public record ProductDraft(
            @NotBlank @Size(max = 200) String title,
            String description,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal price,
            @Size(max = 100) String category,
            @NotBlank @Size(max = 30) String condition,
            @Size(max = 100) String location,
            @NotNull List<@NotNull @Min(0) Integer> imageIndexes
    ) {}
}
