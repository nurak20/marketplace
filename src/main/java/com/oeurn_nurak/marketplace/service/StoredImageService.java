package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.entity.StoredImage;
import com.oeurn_nurak.marketplace.exception.BadRequestException;
import com.oeurn_nurak.marketplace.exception.ResourceNotFoundException;
import com.oeurn_nurak.marketplace.repository.StoredImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/** Stores uploaded images as rows so they survive container restarts and redeploys. */
@Service
@RequiredArgsConstructor
@Transactional
public class StoredImageService {
    private final StoredImageRepository images;

    public String savePng(BufferedImage image, String urlPrefix) {
        UUID id = UUID.randomUUID();
        images.saveAndFlush(StoredImage.builder()
                .id(id)
                .contentType("image/png")
                .data(encode(image))
                .build());
        return urlPrefix + id + ".png";
    }

    @Transactional(readOnly = true)
    public StoredImage load(String filename) {
        return images.findById(requireId(filename))
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + filename));
    }

    public void deleteByUrl(String url) {
        if (url == null) return;
        try {
            images.findById(UUID.fromString(fileName(url))).ifPresent(images::delete);
        } catch (IllegalArgumentException ignored) {
            // Not an id-backed image; nothing to remove.
        }
    }

    private static byte[] encode(BufferedImage image) {
        var output = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, "png", output)) {
                throw new BadRequestException("PNG encoder is unavailable");
            }
        } catch (IOException error) {
            throw new BadRequestException("Unable to store image");
        }
        return output.toByteArray();
    }

    private static UUID requireId(String filename) {
        try {
            return UUID.fromString(fileName(filename));
        } catch (IllegalArgumentException error) {
            throw new ResourceNotFoundException("Image not found: " + filename);
        }
    }

    private static String fileName(String url) {
        String name = url.substring(url.lastIndexOf('/') + 1);
        return name.endsWith(".png") ? name.substring(0, name.length() - ".png".length()) : name;
    }
}
