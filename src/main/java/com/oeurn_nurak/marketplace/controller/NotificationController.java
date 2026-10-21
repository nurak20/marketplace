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
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifications;
    @GetMapping
    @PreAuthorize("hasAuthority('notification:read')")
    public ResponseDto<?> list(Pageable page) { return ResponseDto.ok(notifications.list(page)); }
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('notification:read')")
    public ResponseDto<?> read(@PathVariable Long id) { return ResponseDto.ok(notifications.read(id)); }
    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('notification:read')")
    public ResponseDto<?> readAll() { notifications.readAll(); return ResponseDto.message("Notifications marked read"); }

}
