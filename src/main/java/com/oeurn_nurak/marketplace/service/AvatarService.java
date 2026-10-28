package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.response.AccountResponse;
import com.oeurn_nurak.marketplace.entity.User;
import com.oeurn_nurak.marketplace.exception.BadRequestException;
import com.oeurn_nurak.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AvatarService {
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;
    private static final String URL_PREFIX = "/api/v1/uploads/profile/";
    private static final Set<String> FORMATS = Set.of("png", "jpeg", "jpg", "gif");

    private final CurrentUser current;
    private final UserRepository users;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public AccountResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("Profile image must be between 1 byte and 10 MB");
        }

        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path profileDir = uploadRoot.resolve("profile").normalize();
        if (!profileDir.startsWith(uploadRoot)) throw new IllegalStateException("Invalid upload directory");

        String filename = UUID.randomUUID() + ".png";
        Path target = profileDir.resolve(filename);
        User user = current.get();
        String previousUrl = user.getAvatarUrl();

        try {
            BufferedImage image = readImage(file.getBytes());
            Files.createDirectories(profileDir);
            if (!ImageIO.write(image, "png", target.toFile())) {
                throw new IOException("PNG encoder is unavailable");
            }
            user.setAvatarUrl(URL_PREFIX + filename);
            AccountResponse response = AccountResponse.from(users.saveAndFlush(user));
            removePreviousImage(previousUrl, profileDir, target);
            return response;
        } catch (IOException error) {
            deleteQuietly(target);
            throw new BadRequestException("Unable to read or store profile image");
        } catch (RuntimeException error) {
            deleteQuietly(target);
            throw error;
        }
    }

    private BufferedImage readImage(byte[] data) throws IOException {
        int orientation = ImageOrientation.read(data);
        try (InputStream source = new ByteArrayInputStream(data);
             ImageInputStream input = ImageIO.createImageInputStream(source)) {
            if (input == null) throw new BadRequestException("Invalid image");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BadRequestException("Upload a PNG, JPEG, or GIF image");

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!FORMATS.contains(format)) {
                    throw new BadRequestException("Upload a PNG, JPEG, or GIF image");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((long) width * height > MAX_IMAGE_PIXELS) {
                    throw new BadRequestException("Image exceeds 20 million pixels");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw new BadRequestException("Invalid image");
                return ImageOrientation.apply(image, orientation);
            } finally {
                reader.dispose();
            }
        }
    }

    private void removePreviousImage(String previousUrl, Path profileDir, Path replacement) {
        if (previousUrl == null || !previousUrl.startsWith(URL_PREFIX)) return;
        Path previous = profileDir.resolve(previousUrl.substring(URL_PREFIX.length())).normalize();
        if (!profileDir.equals(previous.getParent()) || previous.equals(replacement)) return;
        deleteQuietly(previous);
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A stale avatar file is harmless; preserve the API response.
        }
    }
}
