package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.entity.User;
import com.oeurn_nurak.marketplace.exception.ForbiddenException;
import com.oeurn_nurak.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final UserRepository users;

    public User get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return users.findByEmail(auth.getName()).filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Account unavailable"));
    }

    public Long id() { return get().getId(); }

    public void requireCanSell() {
        if (!Boolean.TRUE.equals(get().getCanSell())) {
            throw new ForbiddenException("Your selling access has been disabled. Contact the marketplace team.");
        }
    }

    public void requireOwner(Long ownerId) {
        if (!id().equals(ownerId)) throw new ForbiddenException("You do not own this resource");
    }
}
