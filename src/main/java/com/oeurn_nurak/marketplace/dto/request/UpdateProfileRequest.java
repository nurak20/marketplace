package com.oeurn_nurak.marketplace.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 50)  private String firstName;
    @Size(max = 50)  private String lastName;
    @Size(max = 100) private String fullName;
    @Email @Size(max = 100) private String email;
    @Size(max = 20)  private String phone;
    @Size(max = 100) private String location;
    private String bio;
}
