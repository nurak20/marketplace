package com.oeurn_nurak.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "marketplace_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceCondition {
    @Id
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
