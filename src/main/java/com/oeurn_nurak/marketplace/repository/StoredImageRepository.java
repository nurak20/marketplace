package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.StoredImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoredImageRepository extends JpaRepository<StoredImage, UUID> {
}
