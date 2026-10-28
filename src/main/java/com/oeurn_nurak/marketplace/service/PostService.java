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
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final ListingPostRepository posts;
    private final ListingPostItemRepository items;
    private final ProductRepository products;
    private final ProductImageRepository productImages;
    private final WishlistRepository wishlists;
    private final PostReactionRepository reactions;
    private final CommentRepository comments;
    private final ProductService productService;
    private final ImageService imageService;
    private final CurrentUser current;
    private final NotificationService notifications;
    private final UserRepository users;
    private final MarketplaceCatalogService catalog;

    public ListingPost require(Long id) {
        return posts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post", id));
    }

    private PostResponse response(ListingPost post) {
        return response(post, viewerId());
    }

    private PostResponse response(ListingPost post, Long viewerId) {
        List<Product> postProducts = productsForPosts(List.of(post))
                .getOrDefault(post.getId(), List.of());
        Map<Long, List<ProductImage>> productImagesById = imagesByProductId(postProducts);
        Map<Long, UserResponse> sellers = sellerProfiles(postProducts);
        Set<Long> savedProductIds = savedProductIds(postProducts, viewerId);
        return response(post, viewerId, postProducts, productImagesById, sellers, savedProductIds);
    }

    private PostResponse response(
            ListingPost post,
            Long viewerId,
            List<Product> postProducts,
            Map<Long, List<ProductImage>> productImagesById,
            Map<Long, UserResponse> sellers,
            Set<Long> savedProductIds
    ) {
        boolean removed = post.getDeletedAt() != null;
        boolean hasReacted = !removed && viewerId != null
                && reactions.existsByPostIdAndProductIdIsNullAndUserId(post.getId(), viewerId);
        List<ProductEngagementResponse> productEngagements = postProducts.stream()
                .map(product -> new ProductEngagementResponse(product.getId(),
                        reactions.countByPostIdAndProductId(post.getId(), product.getId()),
                        comments.countByPostIdAndProductId(post.getId(), product.getId()),
                        viewerId != null && reactions.existsByPostIdAndProductIdAndUserId(
                                post.getId(), product.getId(), viewerId)))
                .toList();
        List<ProductResponse> productResponses = postProducts.stream()
                .map(product -> {
                    List<ProductImage> productImages = productImagesById
                            .getOrDefault(product.getId(), List.of());
                    return ProductResponse.from(
                            product,
                            primaryImageUrl(productImages),
                            productImages,
                            sellers.get(product.getSellerId()),
                            savedProductIds.contains(product.getId()));
                })
                .toList();
        UserResponse postAuthor = users.findById(post.getUserId()).map(UserResponse::from).orElse(null);
        return new PostResponse(PostSummaryResponse.from(post), removed ? List.of() : productResponses,
                removed ? 0 : reactions.countByPostId(post.getId()),
                removed ? 0 : comments.countByPostId(post.getId()), hasReacted, post.getCreatedAt(),
                removed ? List.of() : productEngagements, postAuthor);
    }

    private Map<Long, List<Product>> productsForPosts(List<ListingPost> pagePosts) {
        if (pagePosts.isEmpty()) return Map.of();

        List<Long> postIds = pagePosts.stream().map(ListingPost::getId).toList();
        Map<Long, List<Long>> productIdsByPost = new HashMap<>();
        pagePosts.forEach(post -> productIdsByPost.put(post.getId(), new ArrayList<>()));
        items.findByPostIdInOrderByIdAsc(postIds).forEach(item ->
                productIdsByPost.get(item.getPostId()).add(item.getProductId()));

        Set<Long> productIds = new LinkedHashSet<>();
        productIdsByPost.values().forEach(productIds::addAll);
        Map<Long, Product> productsById = new HashMap<>();
        products.findAllById(productIds).forEach(product -> productsById.put(product.getId(), product));

        Map<Long, List<Product>> result = new LinkedHashMap<>();
        productIdsByPost.forEach((postId, ids) -> result.put(postId, ids.stream()
                .map(productsById::get)
                .filter(Objects::nonNull)
                .toList()));
        return result;
    }

    private Map<Long, List<ProductImage>> imagesByProductId(List<Product> pageProducts) {
        Set<Long> productIds = pageProducts.stream()
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (productIds.isEmpty()) return Map.of();

        Map<Long, List<ProductImage>> result = new HashMap<>();
        productImages.findByProductIdIn(productIds).stream()
                .sorted(Comparator.comparing(ProductImage::getId))
                .forEach(image -> result.computeIfAbsent(image.getProductId(), ignored -> new ArrayList<>()).add(image));
        return result;
    }

    private String primaryImageUrl(List<ProductImage> images) {
        return images.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0).getImageUrl());
    }

    private Map<Long, UserResponse> sellerProfiles(List<Product> pageProducts) {
        Set<Long> sellerIds = pageProducts.stream()
                .map(Product::getSellerId)
                .collect(java.util.stream.Collectors.toSet());
        if (sellerIds.isEmpty()) return Map.of();

        Map<Long, UserResponse> result = new HashMap<>();
        users.findAllById(sellerIds).forEach(user -> result.put(user.getId(), UserResponse.from(user)));
        return result;
    }

    private Set<Long> savedProductIds(List<Product> pageProducts, Long viewerId) {
        if (viewerId == null || pageProducts.isEmpty()) return Set.of();
        Set<Long> productIds = pageProducts.stream()
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toSet());
        return wishlists.findByUserIdAndProductIdIn(viewerId, productIds).stream()
                .map(Wishlist::getProductId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Long viewerId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        return users.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id) { return response(require(id)); }

    @Transactional(readOnly = true)
    public ProductDetailResponse productDetail(Long postId, Long productId) {
        ListingPost post = require(postId);
        requireAvailable(post);
        requirePostProduct(postId, productId);
        Long viewerId = viewerId();
        List<Comment> all = comments.findByPostIdAndProductIdOrderByCreatedAtAsc(postId, productId);
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
        List<CommentResponse> commentResponses = all.stream()
                .filter(comment -> comment.getParentId() == null)
                .map(comment -> commentResponse(comment, authors, repliesByParent))
                .toList();
        ProductDetailResponse base = productService.detailPage(productId);
        boolean hasLiked = viewerId != null
                && reactions.existsByPostIdAndProductIdAndUserId(postId, productId, viewerId);
        UserResponse postAuthor = users.findById(post.getUserId()).map(UserResponse::from).orElse(null);
        return new ProductDetailResponse(
                base.product(),
                reactions.countByPostIdAndProductId(postId, productId),
                comments.countByPostIdAndProductId(postId, productId),
                hasLiked,
                postAuthor,
                commentResponses,
                base.reviews());
    }

    @Transactional(readOnly = true)
    public MarketplacePageResponse<PostResponse> list(Long userId, Pageable page) {
        Long viewerId = viewerId();
        Page<ListingPost> result = userId == null ? posts.findAll(page) : posts.findByUserIdOrderByCreatedAtDesc(userId, page);
        Map<Long, List<Product>> productsByPost = productsForPosts(result.getContent());
        List<Product> pageProducts = productsByPost.values().stream()
                .flatMap(Collection::stream)
                .toList();
        Map<Long, List<ProductImage>> productImagesById = imagesByProductId(pageProducts);
        Map<Long, UserResponse> sellers = sellerProfiles(pageProducts);
        Set<Long> savedProductIds = savedProductIds(pageProducts, viewerId);
        Page<PostResponse> mapped = result.map(post -> response(
                post,
                viewerId,
                productsByPost.getOrDefault(post.getId(), List.of()),
                productImagesById,
                sellers,
                savedProductIds));
        return MarketplacePageResponse.from(mapped,
                catalog.categories().stream().map(MarketplaceCategory::getName).toList(),
                catalog.conditions().stream().map(MarketplaceCondition::getCode).toList());
    }

    @Transactional(readOnly = true)
    public PageDto<PostModerationResponse> moderationList(Pageable page) {
        return PageDto.from(posts.findAll(page).map(PostModerationResponse::from));
    }

    public PostModerationResponse setAvailable(Long id, boolean available) {
        ListingPost post = require(id);
        if (available) {
            post.setDeletedAt(null);
        } else if (post.getDeletedAt() == null) {
            post.setDeletedAt(LocalDateTime.now());
        }
        return PostModerationResponse.from(posts.save(post));
    }

    private List<Long> validateProducts(CreatePostRequest request) {
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(request.getProductIds()));
        for (Long id : ids) {
            if (id == null || id <= 0) throw new BadRequestException("Invalid product ID");
            current.requireOwner(productService.require(id).getSellerId());
        }
        return ids;
    }

    public PostResponse createListing(CreateListingRequest request, List<MultipartFile> files) {
        if (request == null || request.products() == null || request.products().isEmpty()) {
            throw new BadRequestException("At least one product is required");
        }
        List<MultipartFile> imageFiles = files == null ? List.of() : files;
        Set<Integer> usedImageIndexes = new HashSet<>();
        for (CreateListingRequest.ProductDraft draft : request.products()) {
            for (Integer imageIndex : draft.imageIndexes()) {
                if (imageIndex == null || imageIndex < 0 || imageIndex >= imageFiles.size()) {
                    throw new BadRequestException("Product image index is invalid");
                }
                usedImageIndexes.add(imageIndex);
            }
        }
        if (usedImageIndexes.size() != imageFiles.size()) {
            throw new BadRequestException("Every uploaded image must belong to a product");
        }

        List<Long> productIds = new ArrayList<>();
        for (CreateListingRequest.ProductDraft draft : request.products()) {
            CreateProductRequest productRequest = new CreateProductRequest();
            productRequest.setTitle(draft.title());
            productRequest.setDescription(draft.description());
            productRequest.setPrice(draft.price());
            productRequest.setCategory(draft.category());
            productRequest.setCondition(draft.condition());
            productRequest.setLocation(draft.location());
            Product product = productService.create(productRequest);
            productIds.add(product.getId());

            for (Integer imageIndex : draft.imageIndexes()) {
                imageService.upload(product.getId(), imageFiles.get(imageIndex));
            }
        }

        CreatePostRequest postRequest = new CreatePostRequest();
        postRequest.setContent(request.content());
        postRequest.setProductIds(productIds);
        return create(postRequest);
    }

    public PostResponse create(CreatePostRequest request) {
        current.requireCanSell();
        List<Long> ids = validateProducts(request);
        ListingPost post = posts.save(ListingPost.builder().userId(current.id()).content(request.getContent()).build());
        for (Long id : ids) items.save(ListingPostItem.builder().postId(post.getId()).productId(id).build());
        Long firstProductId = ids.isEmpty() ? null : ids.get(0);
        notifications.notifyPostCreated(post, firstProductId, ids.size(),
                postSummary(request.getContent(), firstProductId));
        return response(post);
    }

    private String postSummary(String content, Long firstProductId) {
        if (content != null && !content.isBlank()) return content;
        return firstProductId == null ? null : productService.require(firstProductId).getTitle();
    }

    public PostResponse update(Long id, CreatePostRequest request) {
        ListingPost post = require(id);
        requireAvailable(post);
        current.requireCanSell();
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
        requireAvailable(post);
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
                "New product reaction", id, id, productId, 1);
        return saved;
    }

    public void unreact(Long id) {
        unreact(id, null);
    }

    public void unreact(Long id, Long productId) {
        requireAvailable(require(id));
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
        requireAvailable(require(id));
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
        requireAvailable(post);
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
        notifications.send(recipientId, userId, NotifType.COMMENT, request.getContent(), id, id, productId, 1);
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
        requireAvailable(require(comment.getPostId()));
        return comment;
    }

    private void requireAvailable(ListingPost post) {
        if (post.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Listing is no longer available");
        }
    }

    public void deleteComment(Long id) {
        Comment comment = requireComment(id);
        current.requireOwner(comment.getUserId());
        comment.setDeletedAt(LocalDateTime.now());
        comments.save(comment);
    }
}
