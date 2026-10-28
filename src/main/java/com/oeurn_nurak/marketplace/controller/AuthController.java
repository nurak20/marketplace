package com.oeurn_nurak.marketplace.controller;

import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.*;
import com.oeurn_nurak.marketplace.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accounts;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseDto<?> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseDto.created("Account created", accounts.register(request));
    }

    @PostMapping("/login")
    public ResponseDto<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseDto.ok(accounts.login(request));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseDto<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accounts.changePassword(request);
        return ResponseDto.message("Password changed");
    }

}
