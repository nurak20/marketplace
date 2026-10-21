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
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final ProductService products;
    @GetMapping
    @PreAuthorize("hasAuthority('wishlist:write')")
    public ResponseDto<?> list(Pageable page) { return ResponseDto.ok(products.wishlist(page)); }
    @PutMapping("/{productId}")
    @PreAuthorize("hasAuthority('wishlist:write')")
    public ResponseDto<?> save(@PathVariable Long productId) { return ResponseDto.ok(products.saveWishlist(productId)); }
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAuthority('wishlist:write')")
    public ResponseDto<?> delete(@PathVariable Long productId) { products.removeWishlist(productId); return ResponseDto.message("Wishlist item removed"); }

}
