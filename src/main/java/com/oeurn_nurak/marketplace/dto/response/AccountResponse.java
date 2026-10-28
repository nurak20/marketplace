package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.User;
import java.util.Set;
import java.util.TreeSet;

public record AccountResponse(UserResponse profile, String email, String phone, boolean active,
                              boolean canSell, Set<String> roles, Set<String> permissions) {
    public static AccountResponse from(User user) {
        Set<String> permissions = new TreeSet<>();
        user.getRoles().forEach(role -> role.getPermissions().forEach(permission ->
                permissions.add(permission.getName())));
        return new AccountResponse(UserResponse.from(user), user.getEmail(), user.getPhone(),
                Boolean.TRUE.equals(user.getIsActive()), Boolean.TRUE.equals(user.getCanSell()),
                user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toSet()),
                permissions);
    }
}
