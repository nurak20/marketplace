package com.oeurn_nurak.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Wrapper for JWT token returned after successful login.
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
}
