package com.oeurn_nurak.marketplace.dto.response;

import com.oeurn_nurak.marketplace.entity.User;
import java.time.LocalDateTime;

/** Public profile: credentials and private contact details never leave the API. */
public record UserResponse(Long id, String username, String fullName, String avatarUrl,
                           String bio, String location, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(),
                user.getAvatarUrl(), user.getBio(), user.getLocation(), user.getCreatedAt());
    }
}
