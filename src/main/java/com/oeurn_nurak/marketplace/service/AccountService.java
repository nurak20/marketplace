package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.AuthResponse;
import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.*;
import com.oeurn_nurak.marketplace.entity.*;
import com.oeurn_nurak.marketplace.exception.*;
import com.oeurn_nurak.marketplace.repository.*;
import com.oeurn_nurak.marketplace.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetails;
    private final JwtService jwt;
    private final CurrentUser current;

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = request.getUsername().trim();
        if (username.length() < 3) throw new BadRequestException("Username must have at least 3 characters");
        if (users.existsByEmail(email) || users.existsByUsername(username))
            throw new ConflictException("Email or username already exists");
        Role role = roles.findByName("ROLE_USER").orElseThrow(() -> new IllegalStateException("Default role missing"));
        users.saveAndFlush(User.builder().username(username).email(email)
                .password(passwords.encode(request.getPassword())).fullName(request.getFullName())
                .isActive(true).roles(new HashSet<>(Set.of(role))).build());
        return new AuthResponse(jwt.generateToken(userDetails.loadUserByUsername(email)));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().trim().toLowerCase(Locale.ROOT)
                : users.findByUsername(request.getUsername().trim()).map(User::getEmail)
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        return new AuthResponse(jwt.generateToken((org.springframework.security.core.userdetails.UserDetails)
                authentication.getPrincipal()));
    }

    public User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public UserResponse profile(Long id) { return UserResponse.from(requireUser(id)); }

    @Transactional(readOnly = true)
    public AccountResponse me() { return AccountResponse.from(current.get()); }

    @Transactional(readOnly = true)
    public Set<String> myPermissions() {
        Set<String> permissions = new TreeSet<>();
        current.get().getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getName())));
        return permissions;
    }

    public AccountResponse update(UpdateProfileRequest request) {
        User user = current.get();
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getBio() != null) user.setBio(request.getBio());
        return AccountResponse.from(users.save(user));
    }

    @Transactional(readOnly = true)
    public PageDto<AccountResponse> list(Pageable pageable) {
        return PageDto.from(users.findAll(pageable).map(AccountResponse::from));
    }

    public AccountResponse setActive(Long id, boolean active) {
        if (current.id().equals(id) && !active) throw new BadRequestException("You cannot deactivate your own account");
        User user = requireUser(id);
        user.setIsActive(active);
        return AccountResponse.from(users.save(user));
    }
}
