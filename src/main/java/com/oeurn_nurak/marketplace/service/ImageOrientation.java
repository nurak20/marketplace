package com.oeurn_nurak.marketplace.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Applies the EXIF orientation flag before an upload is re-encoded to PNG.
 * Java's JPEG reader ignores the flag, so without this a phone photo that a
 * browser or Flutter preview shows mirrored is stored mirrored as well.
 */
public final class ImageOrientation {
    private ImageOrientation() {
    }

    /** Reads the EXIF orientation (1–8) from JPEG bytes; returns 1 when absent or invalid. */
    public static int read(byte[] data) {
        if (data == null || data.length < 4 || (data[0] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xD8) {
            return 1;
        }
        int offset = 2;
        while (offset + 4 <= data.length) {
            if ((data[offset] & 0xFF) != 0xFF) {
                offset++;
                continue;
            }
            int marker = data[offset + 1] & 0xFF;
            if (marker == 0xFF) {
                offset++;
                continue;
            }
            if (marker == 0xDA || marker == 0xD9) break;
            int length = u16(data, offset + 2, false);
            if (length < 2 || offset + 2 + length > data.length) break;
            int payload = offset + 4;
            if (marker == 0xE1 && length >= 8
                    && data[payload] == 'E' && data[payload + 1] == 'x'
                    && data[payload + 2] == 'i' && data[payload + 3] == 'f'
                    && data[payload + 4] == 0 && data[payload + 5] == 0) {
                return tiffOrientation(data, payload + 6, offset + 2 + length);
            }
            offset += 2 + length;
        }
        return 1;
    }

    /** Rewrites the bitmap so its pixels match the view intended by the EXIF flag. */
    public static BufferedImage apply(BufferedImage image, int orientation) {
        if (image == null || orientation < 2 || orientation > 8) return image;
        int width = image.getWidth();
        int height = image.getHeight();
        int outputWidth = orientation >= 5 ? height : width;
        int outputHeight = orientation >= 5 ? width : height;
        AffineTransform transform = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, height);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, height, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, height, width);
            default -> new AffineTransform(0, -1, 1, 0, 0, width);
        };
        BufferedImage output = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(image, transform, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private static int tiffOrientation(byte[] data, int start, int end) {
        if (start + 8 > end) return 1;
        boolean little;
        if (data[start] == 'I' && data[start + 1] == 'I') little = true;
        else if (data[start] == 'M' && data[start + 1] == 'M') little = false;
        else return 1;
        if (u16(data, start + 2, little) != 42) return 1;
        long directory = u32(data, start + 4, little);
        if (directory < 8 || directory > end - start - 2) return 1;
        int entries = start + (int) directory;
        int count = u16(data, entries, little);
        for (int i = 0; i < count; i++) {
            int entry = entries + 2 + i * 12;
            if (entry + 12 > end) return 1;
            if (u16(data, entry, little) == 0x0112) {
                int value = u16(data, entry + 8, little);
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }

    private static int u16(byte[] data, int offset, boolean little) {
        int first = data[offset] & 0xFF;
        int second = data[offset + 1] & 0xFF;
        return little ? (second << 8) | first : (first << 8) | second;
    }

    private static long u32(byte[] data, int offset, boolean little) {
        long value = 0;
        for (int i = 0; i < 4; i++) {
            value = (value << 8) | (data[offset + (little ? 3 - i : i)] & 0xFF);
        }
        return value;
    }
}
