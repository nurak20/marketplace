package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 100) private String fullName;
    @Size(max = 100) private String location;
    @Size(max = 20)  private String phone;
    private String bio;
}
