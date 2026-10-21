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
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final SocialService social;
    @GetMapping
    @PreAuthorize("hasAuthority('friend:write')")
    public ResponseDto<?> list(Pageable page) { return ResponseDto.ok(social.friends(page)); }
    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('friend:write')")
    public ResponseDto<?> requests(Pageable page) { return ResponseDto.ok(social.requests(page)); }
    @PostMapping("/requests/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('friend:write')")
    public ResponseDto<?> request(@PathVariable Long userId) { return ResponseDto.created("Friend request created", social.request(userId)); }
    @PatchMapping("/requests/{id}/accept")
    @PreAuthorize("hasAuthority('friend:write')")
    public ResponseDto<?> accept(@PathVariable Long id) { return ResponseDto.ok(social.respond(id, com.oeurn_nurak.marketplace.entity.enums.FriendStatus.ACCEPTED)); }
    @PatchMapping("/requests/{id}/decline")
    @PreAuthorize("hasAuthority('friend:write')")
    public ResponseDto<?> decline(@PathVariable Long id) { return ResponseDto.ok(social.respond(id, com.oeurn_nurak.marketplace.entity.enums.FriendStatus.DECLINED)); }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('friend:write')")
    public ResponseDto<?> delete(@PathVariable Long id) { social.removeFriend(id); return ResponseDto.message("Friendship removed"); }

}
