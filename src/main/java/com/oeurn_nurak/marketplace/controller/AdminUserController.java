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
    private final AccessService access;
    @GetMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseDto<?> list(Pageable page) { return ResponseDto.ok(accounts.list(page)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('admin:access')")
    public ResponseDto<?> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseDto.created("User created", accounts.createManagedUser(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseDto<?> get(@PathVariable Long id) { return ResponseDto.ok(AccountResponse.from(accounts.requireUser(id))); }
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseDto<?> active(@PathVariable Long id, @Valid @RequestBody UpdateUserAccessRequest request) {
        return ResponseDto.ok(accounts.setActive(id, request.active()));
    }

    @PatchMapping("/{id}/selling")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseDto<?> selling(@PathVariable Long id, @Valid @RequestBody UpdateSellingRequest request) {
        return ResponseDto.ok(accounts.setCanSell(id, request.canSell()));
    }

    @PatchMapping("/{id}/staff-role")
    @PreAuthorize("hasAuthority('admin:access')")
    public ResponseDto<?> staffRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseDto.ok(access.setStaffRole(id, request.roleName()));
    }

}
