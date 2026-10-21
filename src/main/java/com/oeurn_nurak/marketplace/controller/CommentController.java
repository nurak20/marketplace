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
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final PostService posts;
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('comment:write')")
    public ResponseDto<?> update(@PathVariable Long id, @Valid @RequestBody CreateCommentRequest request) {
        return ResponseDto.ok(posts.updateComment(id, request));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('comment:write')")
    public ResponseDto<?> delete(@PathVariable Long id) { posts.deleteComment(id); return ResponseDto.message("Comment deleted"); }

}
