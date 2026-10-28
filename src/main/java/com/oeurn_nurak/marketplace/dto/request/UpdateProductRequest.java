package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
    @Size(max = 200) private String title;
    private String description;
    @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 10, fraction = 2) private BigDecimal price;
    @Size(max = 100) private String category;
    @Size(max = 100) private String location;
    @Size(max = 30) private String condition;
}
