package com.oeurn_nurak.marketplace.controller;

import com.oeurn_nurak.marketplace.entity.StoredImage;
import com.oeurn_nurak.marketplace.service.StoredImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Public image delivery; uploads stay open to anonymous readers. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Images", description = "Public delivery of uploaded product photos and avatars")
public class UploadController {

    private final StoredImageService storage;

    @GetMapping({"/api/v1/uploads/{file}", "/api/v1/uploads/profile/{file}"})
    @Operation(summary = "Download an uploaded image")
    public ResponseEntity<byte[]> image(@PathVariable String file) {
        StoredImage image = storage.load(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .body(image.getData());
    }
}
