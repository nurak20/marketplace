package com.oeurn_nurak.marketplace.controller;

import com.oeurn_nurak.marketplace.dto.request.MarketplaceCategoryRequest;
import com.oeurn_nurak.marketplace.dto.request.MarketplaceConditionRequest;
import com.oeurn_nurak.marketplace.dto.request.UpdateActiveRequest;
import com.oeurn_nurak.marketplace.dto.request.UpdatePostAvailabilityRequest;
import com.oeurn_nurak.marketplace.dto.response.ResponseDto;
import com.oeurn_nurak.marketplace.service.MarketplaceCatalogService;
import com.oeurn_nurak.marketplace.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminManagementController {
    private final MarketplaceCatalogService catalog;
    private final PostService posts;

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('category:manage')")
    public ResponseDto<?> categories() {
        return ResponseDto.ok(catalog.allCategories());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('category:manage')")
    public ResponseDto<?> createCategory(@Valid @RequestBody MarketplaceCategoryRequest request) {
        return ResponseDto.created("Category created", catalog.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('category:manage')")
    public ResponseDto<?> renameCategory(@PathVariable Long id,
                                         @Valid @RequestBody MarketplaceCategoryRequest request) {
        return ResponseDto.ok(catalog.renameCategory(id, request));
    }

    @PatchMapping("/categories/{id}/active")
    @PreAuthorize("hasAuthority('category:manage')")
    public ResponseDto<?> setCategoryActive(@PathVariable Long id,
                                            @Valid @RequestBody UpdateActiveRequest request) {
        return ResponseDto.ok(catalog.setCategoryActive(id, request.active()));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('category:manage')")
    public ResponseDto<?> archiveCategory(@PathVariable Long id) {
        return ResponseDto.ok(catalog.archiveCategory(id));
    }

    @GetMapping("/conditions")
    @PreAuthorize("hasAuthority('condition:manage')")
    public ResponseDto<?> conditions() {
        return ResponseDto.ok(catalog.allConditions());
    }

    @PostMapping("/conditions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('condition:manage')")
    public ResponseDto<?> createCondition(@Valid @RequestBody MarketplaceConditionRequest request) {
        return ResponseDto.created("Condition created", catalog.createCondition(request));
    }

    @PutMapping("/conditions/{code}")
    @PreAuthorize("hasAuthority('condition:manage')")
    public ResponseDto<?> renameCondition(@PathVariable String code,
                                          @Valid @RequestBody MarketplaceConditionRequest request) {
        return ResponseDto.ok(catalog.renameCondition(code, request));
    }

    @PatchMapping("/conditions/{code}/active")
    @PreAuthorize("hasAuthority('condition:manage')")
    public ResponseDto<?> setConditionActive(@PathVariable String code,
                                             @Valid @RequestBody UpdateActiveRequest request) {
        return ResponseDto.ok(catalog.setConditionActive(code, request.active()));
    }

    @DeleteMapping("/conditions/{code}")
    @PreAuthorize("hasAuthority('condition:manage')")
    public ResponseDto<?> archiveCondition(@PathVariable String code) {
        return ResponseDto.ok(catalog.archiveCondition(code));
    }

    @GetMapping("/posts")
    @PreAuthorize("hasAuthority('post:moderate')")
    public ResponseDto<?> posts(Pageable page) {
        return ResponseDto.ok(posts.moderationList(page));
    }

    @PatchMapping("/posts/{id}/availability")
    @PreAuthorize("hasAuthority('post:moderate')")
    public ResponseDto<?> setPostAvailability(@PathVariable Long id,
                                              @Valid @RequestBody UpdatePostAvailabilityRequest request) {
        return ResponseDto.ok(posts.setAvailable(id, request.available()));
    }
}
