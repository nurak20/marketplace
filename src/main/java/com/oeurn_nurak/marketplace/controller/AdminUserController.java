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
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AccountService accounts;
    @GetMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseDto<?> list(Pageable page) { return ResponseDto.ok(accounts.list(page)); }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseDto<?> get(@PathVariable Long id) { return ResponseDto.ok(AccountResponse.from(accounts.requireUser(id))); }
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseDto<?> active(@PathVariable Long id, @Valid @RequestBody UpdateUserAccessRequest request) {
        return ResponseDto.ok(accounts.setActive(id, request.active()));
    }

}
