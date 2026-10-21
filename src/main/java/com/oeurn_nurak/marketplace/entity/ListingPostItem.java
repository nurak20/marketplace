package com.oeurn_nurak.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "listing_post_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"post_id","product_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ListingPostItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "product_id", nullable = false)
    private Long productId;
}
