package com.oeurn_nurak.marketplace.controller;

import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.*;
import com.oeurn_nurak.marketplace.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AccountService accounts;
    private final AvatarService avatars;
    private final ProductService products;

    @GetMapping("/me")
    public ResponseDto<?> me() { return ResponseDto.ok(accounts.me()); }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseDto<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseDto.ok(avatars.upload(file));
    }

    @GetMapping("/me/permissions")
    public ResponseDto<?> permissions() { return ResponseDto.ok(accounts.myPermissions()); }

    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseDto<?> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseDto.ok(accounts.update(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseDto<?> profile(@PathVariable Long id) { return ResponseDto.ok(accounts.profile(id)); }

    @GetMapping("/{id}/reviews")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseDto<?> reviews(@PathVariable Long id, Pageable page) {
        return ResponseDto.ok(products.sellerReviews(id, page));
    }

}
