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
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService posts;

    @GetMapping
    public ResponseDto<?> list(@RequestParam(required = false) Long userId, Pageable page) {
        return ResponseDto.ok(posts.list(userId, page));
    }

    @GetMapping("/{id}")
    public ResponseDto<?> get(@PathVariable Long id) { return ResponseDto.ok(posts.get(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('post:write')")
    public ResponseDto<?> create(@Valid @RequestBody CreatePostRequest request) {
        return ResponseDto.created("Post created", posts.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('post:write')")
    public ResponseDto<?> update(@PathVariable Long id, @Valid @RequestBody CreatePostRequest request) {
        return ResponseDto.ok(posts.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('post:write')")
    public ResponseDto<?> delete(@PathVariable Long id) { posts.delete(id); return ResponseDto.message("Post deleted"); }

    @PutMapping("/{id}/reaction")
    @PreAuthorize("hasAuthority('post:write')")
    public ResponseDto<?> react(@PathVariable Long id, @Valid @RequestBody ReactToPostRequest request) {
        return ResponseDto.ok(posts.react(id, request));
    }

    @DeleteMapping("/{id}/reaction")
    @PreAuthorize("hasAuthority('post:write')")
    public ResponseDto<?> unreact(@PathVariable Long id) { posts.unreact(id); return ResponseDto.message("Reaction removed"); }

    @PutMapping("/{postId}/products/{productId}/reaction")
    @PreAuthorize("hasAuthority('post:write')")
    public ResponseDto<?> reactToProduct(@PathVariable Long postId, @PathVariable Long productId,
                                         @Valid @RequestBody ReactToPostRequest request) {
        return ResponseDto.ok(posts.react(postId, productId, request));
    }

    @DeleteMapping("/{postId}/products/{productId}/reaction")
    @PreAuthorize("hasAuthority('post:write')")
    public ResponseDto<?> unreactToProduct(@PathVariable Long postId, @PathVariable Long productId) {
        posts.unreact(postId, productId);
        return ResponseDto.message("Product reaction removed");
    }

    @GetMapping("/{id}/comments")
    public ResponseDto<?> comments(@PathVariable Long id, Pageable page) { return ResponseDto.ok(posts.comments(id, page)); }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('comment:write')")
    public ResponseDto<?> comment(@PathVariable Long id, @Valid @RequestBody CreateCommentRequest request) {
        return ResponseDto.created("Comment created", posts.comment(id, request));
    }

    @GetMapping("/{postId}/products/{productId}/comments")
    public ResponseDto<?> productComments(@PathVariable Long postId, @PathVariable Long productId,
                                          Pageable page) {
        return ResponseDto.ok(posts.comments(postId, productId, page));
    }

    @PostMapping("/{postId}/products/{productId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('comment:write')")
    public ResponseDto<?> commentOnProduct(@PathVariable Long postId, @PathVariable Long productId,
                                           @Valid @RequestBody CreateCommentRequest request) {
        return ResponseDto.created("Comment created", posts.comment(postId, productId, request));
    }

}
