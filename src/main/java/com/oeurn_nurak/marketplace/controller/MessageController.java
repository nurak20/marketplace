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
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final SocialService social;
    @GetMapping
    @PreAuthorize("hasAuthority('message:write')")
    public ResponseDto<?> inbox() { return ResponseDto.ok(social.inbox()); }
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('message:write')")
    public ResponseDto<?> conversation(@PathVariable Long userId, Pageable page) { return ResponseDto.ok(social.conversation(userId, page)); }
    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('message:write')")
    public ResponseDto<?> send(@PathVariable Long userId, @Valid @RequestBody SendMessageRequest request) {
        return ResponseDto.created("Message sent", social.send(userId, request));
    }
    @PatchMapping("/{userId}/read")
    @PreAuthorize("hasAuthority('message:write')")
    public ResponseDto<?> read(@PathVariable Long userId) { social.readConversation(userId); return ResponseDto.message("Conversation marked read"); }

}
