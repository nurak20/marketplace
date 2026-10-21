package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.*;
import com.oeurn_nurak.marketplace.entity.*;
import com.oeurn_nurak.marketplace.entity.enums.*;
import com.oeurn_nurak.marketplace.exception.*;
import com.oeurn_nurak.marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final ListingPostRepository posts;
    private final ListingPostItemRepository items;
    private final ProductRepository products;
    private final PostReactionRepository reactions;
    private final CommentRepository comments;
    private final ProductService productService;
    private final CurrentUser current;
    private final NotificationService notifications;
    private final UserRepository users;

    public ListingPost require(Long id) {
        return posts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post", id));
    }

    private PostResponse response(ListingPost post) {
        return response(post, viewerId());
    }

    private PostResponse response(ListingPost post, Long viewerId) {
        List<Long> ids = items.findByPostIdOrderByIdAsc(post.getId()).stream()
                .map(ListingPostItem::getProductId).toList();
        Map<Long, Product> productsById = products.findAllById(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, product -> product));
        List<Product> postProducts = ids.stream().map(productsById::get).filter(Objects::nonNull).toList();
        boolean hasReacted = viewerId != null
                && reactions.existsByPostIdAndProductIdIsNullAndUserId(post.getId(), viewerId);
        List<ProductEngagementResponse> productEngagements = postProducts.stream()
                .map(product -> new ProductEngagementResponse(product.getId(),
                        reactions.countByPostIdAndProductId(post.getId(), product.getId()),
                        comments.countByPostIdAndProductId(post.getId(), product.getId()),
                        viewerId != null && reactions.existsByPostIdAndProductIdAndUserId(
                                post.getId(), product.getId(), viewerId)))
                .toList();
        return new PostResponse(post, postProducts, reactions.countByPostId(post.getId()),
                comments.countByPostId(post.getId()), hasReacted, post.getCreatedAt(), productEngagements);
    }

    private Long viewerId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        return users.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id) { return response(require(id)); }

    @Transactional(readOnly = true)
    public PageDto<PostResponse> list(Long userId, Pageable page) {
        Long viewerId = viewerId();
        Page<ListingPost> result = userId == null ? posts.findAll(page) : posts.findByUserIdOrderByCreatedAtDesc(userId, page);
        return PageDto.from(result.map(post -> response(post, viewerId)));
    }

    private List<Long> validateProducts(CreatePostRequest request) {
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(request.getProductIds()));
        for (Long id : ids) {
            if (id == null || id <= 0) throw new BadRequestException("Invalid product ID");
            current.requireOwner(productService.require(id).getSellerId());
        }
        return ids;
    }

    public PostResponse create(CreatePostRequest request) {
        List<Long> ids = validateProducts(request);
        ListingPost post = posts.save(ListingPost.builder().userId(current.id()).content(request.getContent()).build());
        for (Long id : ids) items.save(ListingPostItem.builder().postId(post.getId()).productId(id).build());
        return response(post);
    }

    public PostResponse update(Long id, CreatePostRequest request) {
        ListingPost post = require(id);
        current.requireOwner(post.getUserId());
        List<Long> ids = validateProducts(request);
        items.deleteByPostId(id);
        items.flush();
        for (Long productId : ids) items.save(ListingPostItem.builder().postId(id).productId(productId).build());
        post.setContent(request.getContent());
        posts.save(post);
        return response(post);
    }

    public void delete(Long id) {
        ListingPost post = require(id);
        current.requireOwner(post.getUserId());
        post.setDeletedAt(LocalDateTime.now());
        posts.save(post);
    }

    private void requirePostProduct(Long postId, Long productId) {
        if (!items.existsByPostIdAndProductId(postId, productId)) {
            throw new BadRequestException("Product does not belong to this post");
        }
    }

    public PostReaction react(Long id, ReactToPostRequest request) {
        return react(id, null, request);
    }

    public PostReaction react(Long id, Long productId, ReactToPostRequest request) {
        ListingPost post = require(id);
        if (productId != null) requirePostProduct(id, productId);
        Long userId = current.id();
        var existing = productId == null
                ? reactions.findByPostIdAndProductIdIsNullAndUserId(id, userId)
                : reactions.findByPostIdAndProductIdAndUserId(id, productId, userId);
        PostReaction reaction = existing.orElseGet(() -> PostReaction.builder()
                .postId(id).productId(productId).userId(userId).build());
        reaction.setReactionType(request.getReactionType());
        PostReaction saved = reactions.save(reaction);
        if (existing.isEmpty()) notifications.send(post.getUserId(), userId, NotifType.LIKE,
                "New product reaction", id);
        return saved;
    }

    public void unreact(Long id) {
        unreact(id, null);
    }

    public void unreact(Long id, Long productId) {
        require(id);
        if (productId != null) requirePostProduct(id, productId);
        var reaction = productId == null
                ? reactions.findByPostIdAndProductIdIsNullAndUserId(id, current.id())
                : reactions.findByPostIdAndProductIdAndUserId(id, productId, current.id());
        reaction.ifPresent(reactions::delete);
    }

    @Transactional(readOnly = true)
    public PageDto<CommentResponse> comments(Long id, Pageable page) {
        return comments(id, null, page);
    }

    @Transactional(readOnly = true)
    public PageDto<CommentResponse> comments(Long id, Long productId, Pageable page) {
        require(id);
        if (productId != null) requirePostProduct(id, productId);
        Page<Comment> roots = productId == null
                ? comments.findByPostIdAndProductIdIsNullAndParentIdIsNullOrderByCreatedAtAsc(id, page)
                : comments.findByPostIdAndProductIdAndParentIdIsNullOrderByCreatedAtAsc(id, productId, page);
        List<Comment> all = productId == null
                ? comments.findByPostIdAndProductIdIsNullOrderByCreatedAtAsc(id)
                : comments.findByPostIdAndProductIdOrderByCreatedAtAsc(id, productId);
        Map<Long, List<Comment>> repliesByParent = new HashMap<>();
        Set<Long> authorIds = new HashSet<>();
        for (Comment comment : all) {
            authorIds.add(comment.getUserId());
            if (comment.getParentId() != null) {
                repliesByParent.computeIfAbsent(comment.getParentId(), ignored -> new ArrayList<>()).add(comment);
            }
        }
        Map<Long, User> authors = new HashMap<>();
        users.findAllById(authorIds).forEach(user -> authors.put(user.getId(), user));
        return PageDto.from(roots.map(root -> commentResponse(root, authors, repliesByParent)));
    }

    public Comment comment(Long id, CreateCommentRequest request) {
        return comment(id, null, request);
    }

    public Comment comment(Long id, Long productId, CreateCommentRequest request) {
        ListingPost post = require(id);
        if (productId != null) requirePostProduct(id, productId);
        Long userId = current.id();
        Comment parent = request.getParentId() == null ? null : requireComment(request.getParentId());
        if (parent != null && (!parent.getPostId().equals(id)
                || !Objects.equals(parent.getProductId(), productId))) {
            throw new BadRequestException("Parent comment belongs to a different listing product");
        }
        Comment comment = comments.save(Comment.builder().postId(id).productId(productId).userId(userId)
                .parentId(parent == null ? null : parent.getId()).content(request.getContent()).build());
        Long recipientId = parent == null ? post.getUserId() : parent.getUserId();
        String notificationMessage = parent == null ? "New post comment" : "New comment reply";
        notifications.send(recipientId, userId, NotifType.COMMENT, notificationMessage, id);
        return comment;
    }

    private CommentResponse commentResponse(Comment comment, Map<Long, User> authors,
                                            Map<Long, List<Comment>> repliesByParent) {
        User author = authors.get(comment.getUserId());
        List<CommentResponse> replies = repliesByParent.getOrDefault(comment.getId(), List.of()).stream()
                .map(reply -> commentResponse(reply, authors, repliesByParent)).toList();
        return new CommentResponse(comment.getId(), comment.getPostId(), comment.getProductId(), comment.getUserId(),
                comment.getParentId(), comment.getContent(), comment.getCreatedAt(),
                author == null ? null : author.getUsername(),
                author == null ? null : author.getFullName(),
                author == null ? null : author.getAvatarUrl(), replies);
    }

    public Comment updateComment(Long id, CreateCommentRequest request) {
        Comment comment = requireComment(id);
        current.requireOwner(comment.getUserId());
        comment.setContent(request.getContent());
        return comments.save(comment);
    }

    private Comment requireComment(Long id) {
        Comment comment = comments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment", id));
        require(comment.getPostId());
        return comment;
    }

    public void deleteComment(Long id) {
        Comment comment = requireComment(id);
        current.requireOwner(comment.getUserId());
        comment.setDeletedAt(LocalDateTime.now());
        comments.save(comment);
    }
}
