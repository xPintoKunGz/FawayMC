package com.imagery.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;

/**
 * Image processing utilities for scaling, slicing, and analyzing images.
 */
public final class ImageUtil {

    public static final int MAP_RESOLUTION = 128; // Standard Minecraft Map size in pixels

    private ImageUtil() {}

    /**
     * Resizes the source image to target block dimensions (widthBlocks * 128, heightBlocks * 128)
     * using high-fidelity bicubic interpolation.
     */
    public static BufferedImage resize(BufferedImage source, int widthBlocks, int heightBlocks) {
        int targetWidth = Math.max(1, widthBlocks) * MAP_RESOLUTION;
        int targetHeight = Math.max(1, heightBlocks) * MAP_RESOLUTION;

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return scaled;
    }

    /**
     * Slices a scaled image into a 2D array [row][col] of 128x128 pixel tiles.
     * row 0 is top, row heightBlocks - 1 is bottom.
     * col 0 is left, col widthBlocks - 1 is right.
     */
    public static BufferedImage[][] slice(BufferedImage scaledImage, int widthBlocks, int heightBlocks) {
        BufferedImage[][] tiles = new BufferedImage[heightBlocks][widthBlocks];

        for (int row = 0; row < heightBlocks; row++) {
            for (int col = 0; col < widthBlocks; col++) {
                int x = col * MAP_RESOLUTION;
                int y = row * MAP_RESOLUTION;

                BufferedImage tile = new BufferedImage(MAP_RESOLUTION, MAP_RESOLUTION, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = tile.createGraphics();
                g2d.drawImage(scaledImage.getSubimage(x, y, MAP_RESOLUTION, MAP_RESOLUTION), 0, 0, null);
                g2d.dispose();

                tiles[row][col] = tile;
            }
        }

        return tiles;
    }

    /**
     * Suggests a suitable default block width and height based on the image's aspect ratio.
     * Caps maximum default span to maxDim blocks to prevent accidental gigantic placement.
     */
    public static int[] calculateDefaultBlockSize(int imageWidth, int imageHeight, int maxDim) {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return new int[]{2, 2};
        }

        double ratio = (double) imageWidth / (double) imageHeight;
        int w, h;

        if (ratio >= 1.0) {
            w = Math.min(maxDim, Math.max(2, (int) Math.round(2 * ratio)));
            h = Math.max(1, (int) Math.round(w / ratio));
        } else {
            h = Math.min(maxDim, Math.max(2, (int) Math.round(2 / ratio)));
            w = Math.max(1, (int) Math.round(h * ratio));
        }

        return new int[]{Math.max(1, w), Math.max(1, h)};
    }

    /**
     * Formats file bytes into readable human string (KB, MB).
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(units.length - 1, digitGroups);
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}

