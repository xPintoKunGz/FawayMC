package com.imagery.model;

import com.imagery.util.ImageUtil;

import java.io.File;

/**
 * Metadata cache for an image file located in the plugin's images directory.
 */
public class ImageMetadata {

    private final String fileName;
    private final File file;
    private final int pixelWidth;
    private final int pixelHeight;
    private final long fileSize;

    public ImageMetadata(String fileName, File file, int pixelWidth, int pixelHeight, long fileSize) {
        this.fileName = fileName;
        this.file = file;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public File getFile() {
        return file;
    }

    public int getPixelWidth() {
        return pixelWidth;
    }

    public int getPixelHeight() {
        return pixelHeight;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFormattedSize() {
        return ImageUtil.formatFileSize(fileSize);
    }

    public double getAspectRatio() {
        if (pixelHeight == 0) return 1.0;
        return (double) pixelWidth / (double) pixelHeight;
    }

    public int[] getSuggestedBlocks(int maxDim) {
        return ImageUtil.calculateDefaultBlockSize(pixelWidth, pixelHeight, maxDim);
    }
}

