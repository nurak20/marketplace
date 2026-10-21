package com.oeurn_nurak.marketplace.dto.request;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class LoginRequest {
    @Email private String email;
    private String username;
    @NotBlank(message = "Password is required") private String password;
    @AssertTrue(message = "Provide email or username")
    @JsonIgnore
    public boolean isIdentifierProvided() {
        return email != null && !email.isBlank() || username != null && !username.isBlank();
    }
}
