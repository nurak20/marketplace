package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.User;
import java.util.Set;
import java.util.stream.Collectors;

public record AccountResponse(UserResponse profile, String email, String phone, boolean active, Set<String> roles) {
    public static AccountResponse from(User user) {
        return new AccountResponse(UserResponse.from(user), user.getEmail(), user.getPhone(),
                Boolean.TRUE.equals(user.getIsActive()), user.getRoles().stream()
                .map(role -> role.getName()).collect(Collectors.toSet()));
    }
}
