package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.TestImages;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class ImageOrientationTest {

    @Test void readsOrientationFromJpegExif() throws Exception {
        byte[] image = TestImages.jpeg(TestImages.pixels(4, 2, 1, 2, 3, 4, 5, 6, 7, 8));
        assertThat(ImageOrientation.read(TestImages.withExif(image, 2, true))).isEqualTo(2);
        assertThat(ImageOrientation.read(TestImages.withExif(image, 6, true))).isEqualTo(6);
        assertThat(ImageOrientation.read(TestImages.withExif(image, 8, false))).isEqualTo(8);
    }

    @Test void defaultsToNormalWhenTagIsMissingOrInvalid() throws Exception {
        assertThat(ImageOrientation.read(TestImages.jpeg(TestImages.pixels(2, 2, 1, 2, 3, 4)))).isEqualTo(1);
        assertThat(ImageOrientation.read("not an image".getBytes())).isEqualTo(1);
        assertThat(ImageOrientation.read(TestImages.withExif(
                TestImages.jpeg(TestImages.pixels(2, 2, 1, 2, 3, 4)), 9, true))).isEqualTo(1);
        assertThat(ImageOrientation.read(null)).isEqualTo(1);
    }

    @Test void mirrorsHorizontalFlip() {
        BufferedImage result = ImageOrientation.apply(TestImages.pixels(2, 1, 1, 2), 2);
        assertThat(result.getWidth()).isEqualTo(2);
        assertThat(result.getHeight()).isEqualTo(1);
        assertThat(result.getRGB(0, 0) & 0xFFFFFF).isEqualTo(2);
        assertThat(result.getRGB(1, 0) & 0xFFFFFF).isEqualTo(1);
    }

    @Test void rotatesQuarterTurnClockwise() {
        // A B C
        // D E F
        BufferedImage result = ImageOrientation.apply(TestImages.pixels(3, 2, 1, 2, 3, 4, 5, 6), 6);
        assertThat(result.getWidth()).isEqualTo(2);
        assertThat(result.getHeight()).isEqualTo(3);
        // D A
        // E B
        // F C
        assertThat(result.getRGB(0, 0) & 0xFFFFFF).isEqualTo(4);
        assertThat(result.getRGB(1, 0) & 0xFFFFFF).isEqualTo(1);
        assertThat(result.getRGB(0, 1) & 0xFFFFFF).isEqualTo(5);
        assertThat(result.getRGB(1, 1) & 0xFFFFFF).isEqualTo(2);
        assertThat(result.getRGB(0, 2) & 0xFFFFFF).isEqualTo(6);
        assertThat(result.getRGB(1, 2) & 0xFFFFFF).isEqualTo(3);
    }

    @Test void leavesUprightImagesUntouched() {
        BufferedImage source = TestImages.pixels(1, 1, 7);
        assertThat(ImageOrientation.apply(source, 1)).isSameAs(source);
        assertThat(ImageOrientation.apply(source, 0)).isSameAs(source);
    }
}
