package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.PageDto;
import com.oeurn_nurak.marketplace.entity.*;
import com.oeurn_nurak.marketplace.entity.enums.*;
import com.oeurn_nurak.marketplace.exception.*;
import com.oeurn_nurak.marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository products;
    private final ProductImageRepository images;
    private final ReviewRepository reviews;
    private final WishlistRepository wishlists;
    private final CurrentUser current;
    private final AccountService accounts;
    private final NotificationService notifications;

    public Product require(Long id) {
        return products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional(readOnly = true)
    public PageDto<Product> list(String keyword, String category, String location, ConditionType condition,
                                 ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice,
                                 Long sellerId, Pageable page) {
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0
                || minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)
            throw new BadRequestException("Invalid price range");
        Specification<Product> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT).replace("\\", "\\\\")
                        .replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\')));
            }
            if (category != null) predicates.add(cb.equal(root.get("category"), category));
            if (location != null) predicates.add(cb.equal(root.get("location"), location));
            if (condition != null) predicates.add(cb.equal(root.get("condition"), condition));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (minPrice != null) predicates.add(cb.ge(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.le(root.get("price"), maxPrice));
            if (sellerId != null) predicates.add(cb.equal(root.get("sellerId"), sellerId));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return PageDto.from(products.findAll(spec, page));
    }

    public Product create(CreateProductRequest request) {
        if (request.getCondition() == null) throw new BadRequestException("Condition is required");
        return products.save(Product.builder().sellerId(current.id()).title(request.getTitle().trim())
                .description(request.getDescription()).price(request.getPrice()).category(request.getCategory())
                .location(request.getLocation()).condition(request.getCondition()).status(ProductStatus.AVAILABLE).build());
    }

    public Product update(Long id, UpdateProductRequest request) {
        Product product = require(id);
        current.requireOwner(product.getSellerId());
        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) throw new BadRequestException("Title must not be blank");
            product.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getLocation() != null) product.setLocation(request.getLocation());
        if (request.getCondition() != null) product.setCondition(request.getCondition());
        return products.save(product);
    }

    public Product status(Long id, ProductStatus status) {
        Product product = require(id);
        current.requireOwner(product.getSellerId());
        if (status == ProductStatus.DELETED) throw new BadRequestException("Use DELETE to remove a product");
        product.setStatus(status);
        return products.save(product);
    }

    public void delete(Long id) {
        Product product = require(id);
        current.requireOwner(product.getSellerId());
        product.setDeletedAt(LocalDateTime.now());
        product.setStatus(ProductStatus.DELETED);
        products.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductImage> images(Long id) { require(id); return images.findByProductId(id); }

    @Transactional(readOnly = true)
    public PageDto<Review> reviews(Long id, Pageable page) {
        require(id);
        return PageDto.from(reviews.findByProductId(id, page));
    }

    public Review review(Long id, AddReviewRequest request) {
        Product product = require(id);
        Long buyer = current.id();
        if (buyer.equals(product.getSellerId())) throw new BadRequestException("You cannot review your own product");
        if (reviews.existsByProductIdAndBuyerId(id, buyer)) throw new ConflictException("Product already reviewed");
        Review review = reviews.save(Review.builder().productId(id).buyerId(buyer).sellerId(product.getSellerId())
                .rating(request.getRating()).comment(request.getComment()).build());
        notifications.send(product.getSellerId(), buyer, NotifType.REVIEW, "New product review", review.getId());
        return review;
    }

    public void deleteReview(Long id) {
        Review review = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", id));
        current.requireOwner(review.getBuyerId());
        reviews.delete(review);
    }

    @Transactional(readOnly = true)
    public PageDto<Product> wishlist(Pageable page) {
        return PageDto.from(products.findWishlisted(current.id(), page));
    }

    public Wishlist saveWishlist(Long productId) {
        require(productId);
        Long userId = current.id();
        return wishlists.findByUserIdAndProductId(userId, productId).orElseGet(() ->
                wishlists.save(Wishlist.builder().userId(userId).productId(productId).build()));
    }

    public void removeWishlist(Long productId) {
        wishlists.findByUserIdAndProductId(current.id(), productId).ifPresent(wishlists::delete);
    }

    @Transactional(readOnly = true)
    public PageDto<Review> sellerReviews(Long sellerId, Pageable page) {
        accounts.requireUser(sellerId);
        return PageDto.from(reviews.findBySellerId(sellerId, page));
    }
}
