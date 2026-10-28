package com.oeurn_nurak.marketplace;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/** Fixtures for image upload tests, including a JPEG with an EXIF orientation tag. */
public final class TestImages {
    private TestImages() {
    }

    public static BufferedImage pixels(int width, int height, int... values) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < values.length; i++) {
            image.setRGB(i % width, i / width, values[i]);
        }
        return image;
    }

    public static byte[] jpeg(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", out);
        return out.toByteArray();
    }

    /** Injects an APP1/EXIF segment carrying the given orientation right after the JPEG start marker. */
    public static byte[] withExif(byte[] jpeg, int orientation, boolean littleEndian) {
        byte[] exif = new byte[32];
        exif[0] = 'E';
        exif[1] = 'x';
        exif[2] = 'i';
        exif[3] = 'f';
        exif[6] = (byte) (littleEndian ? 'I' : 'M');
        exif[7] = (byte) (littleEndian ? 'I' : 'M');
        word(exif, 8, 42, littleEndian);
        dword(exif, 10, 8, littleEndian);
        word(exif, 14, 1, littleEndian);
        word(exif, 16, 0x0112, littleEndian);
        word(exif, 18, 3, littleEndian);
        dword(exif, 20, 1, littleEndian);
        word(exif, 24, orientation, littleEndian);
        word(exif, 26, 0, littleEndian);
        dword(exif, 28, 0, littleEndian);

        byte[] result = new byte[jpeg.length + exif.length + 4];
        result[0] = (byte) 0xFF;
        result[1] = (byte) 0xD8;
        result[2] = (byte) 0xFF;
        result[3] = (byte) 0xE1;
        int size = exif.length + 2;
        result[4] = (byte) (size >> 8);
        result[5] = (byte) size;
        System.arraycopy(exif, 0, result, 6, exif.length);
        System.arraycopy(jpeg, 2, result, 6 + exif.length, jpeg.length - 2);
        return result;
    }

    private static void word(byte[] data, int offset, int value, boolean little) {
        data[offset] = (byte) (little ? value : value >> 8);
        data[offset + 1] = (byte) (little ? value >> 8 : value);
    }

    private static void dword(byte[] data, int offset, long value, boolean little) {
        for (int i = 0; i < 4; i++) {
            data[offset + i] = (byte) (value >> (8 * (little ? i : 3 - i)));
        }
    }
}
