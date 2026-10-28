package com.oeurn_nurak.marketplace.controller;

import com.oeurn_nurak.marketplace.dto.response.ResponseDto;
import com.oeurn_nurak.marketplace.service.MarketplaceCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MarketplaceCatalogController {
    private final MarketplaceCatalogService catalog;

    @GetMapping("/categories")
    public ResponseDto<?> categories() {
        return ResponseDto.ok(catalog.categories());
    }

    @GetMapping("/conditions")
    public ResponseDto<?> conditions() {
        return ResponseDto.ok(catalog.conditions());
    }

    @GetMapping("/marketplace/options")
    public ResponseDto<?> options() {
        return ResponseDto.ok(com.oeurn_nurak.marketplace.dto.response.MarketplaceOptionsResponse
                .from(catalog.categories(), catalog.conditions()));
    }
}
