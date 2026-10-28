package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.entity.ProductImage;
import com.oeurn_nurak.marketplace.exception.*;
import com.oeurn_nurak.marketplace.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageService {
    private static final String URL_PREFIX = "/api/v1/uploads/";

    private final ProductImageRepository images;
    private final ProductService products;
    private final CurrentUser current;
    private final StoredImageService storage;

    public ProductImage upload(Long productId, MultipartFile file) {
        current.requireCanSell();
        current.requireOwner(products.require(productId).getSellerId());
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) throw new BadRequestException("Image must be 1 byte to 10 MB");
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read image");
        }
        int orientation = ImageOrientation.read(data);
        BufferedImage bitmap;
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (stream == null) throw new BadRequestException("Invalid image");
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new BadRequestException("Upload a PNG, JPEG, or GIF image");
            var reader = readers.next();
            try {
                reader.setInput(stream);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 20_000_000)
                    throw new BadRequestException("Image exceeds 20 million pixels");
                bitmap = ImageOrientation.apply(reader.read(0), orientation);
            } finally { reader.dispose(); }
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read or store image");
        }
        if (bitmap == null) throw new BadRequestException("Invalid image");
        return images.saveAndFlush(ProductImage.builder().productId(productId)
                .imageUrl(storage.savePng(bitmap, URL_PREFIX)).isPrimary(images.findByProductId(productId).isEmpty()).build());
    }

    public ProductImage primary(Long productId, Long imageId) {
        current.requireCanSell();
        current.requireOwner(products.require(productId).getSellerId());
        ProductImage image = require(productId, imageId);
        images.findByProductId(productId).forEach(item -> item.setIsPrimary(item.getId().equals(imageId)));
        return image;
    }

    private ProductImage require(Long productId, Long imageId) {
        return images.findById(imageId).filter(image -> image.getProductId().equals(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Product image", imageId));
    }

    public void delete(Long productId, Long imageId) {
        current.requireOwner(products.require(productId).getSellerId());
        ProductImage image = require(productId, imageId);
        images.delete(image);
        images.flush();
        storage.deleteByUrl(image.getImageUrl());
        if (Boolean.TRUE.equals(image.getIsPrimary())) {
            var remaining = images.findByProductId(productId);
            if (!remaining.isEmpty()) remaining.get(0).setIsPrimary(true);
        }
    }
}
