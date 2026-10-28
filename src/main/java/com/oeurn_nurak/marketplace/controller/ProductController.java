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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService products;
    private final ImageService images;
    private final PostService posts;

    @GetMapping
    public ResponseDto<?> list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category, @RequestParam(required = false) String location,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) com.oeurn_nurak.marketplace.entity.enums.ProductStatus status,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Long sellerId, Pageable page) {
        return ResponseDto.ok(products.list(keyword, category, location, condition, status, minPrice, maxPrice, sellerId, page));
    }

    @GetMapping("/{id}")
    public ResponseDto<?> get(@PathVariable Long id, @RequestParam(required = false) Long postId) {
        return ResponseDto.ok(postId == null
                ? products.detailPage(id)
                : posts.productDetail(postId, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseDto<?> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseDto.created("Product created", products.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseDto<?> update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return ResponseDto.ok(products.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseDto<?> status(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseDto.ok(products.status(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseDto<?> delete(@PathVariable Long id) { products.delete(id); return ResponseDto.message("Product deleted"); }

    @GetMapping("/{id}/images")
    public ResponseDto<?> images(@PathVariable Long id) { return ResponseDto.ok(products.images(id)); }

    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseDto<?> upload(@PathVariable Long id, @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseDto.created("Image uploaded", images.upload(id, file));
    }

    @PatchMapping("/{id}/images/{imageId}/primary")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseDto<?> primary(@PathVariable Long id, @PathVariable Long imageId) {
        return ResponseDto.ok(images.primary(id, imageId));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseDto<?> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        images.delete(id, imageId); return ResponseDto.message("Image deleted");
    }

    @GetMapping("/{id}/reviews")
    public ResponseDto<?> reviews(@PathVariable Long id, Pageable page) { return ResponseDto.ok(products.reviews(id, page)); }

    @PostMapping("/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('review:write')")
    public ResponseDto<?> review(@PathVariable Long id, @Valid @RequestBody AddReviewRequest request) {
        return ResponseDto.created("Review created", products.review(id, request));
    }

}
