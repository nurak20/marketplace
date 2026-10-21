package com.oeurn_nurak.marketplace.dto.request;

import com.oeurn_nurak.marketplace.entity.enums.ConditionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @Size(max = 100) private String category;
    @Size(max = 100) private String location;

    private ConditionType condition = ConditionType.GOOD;
}
