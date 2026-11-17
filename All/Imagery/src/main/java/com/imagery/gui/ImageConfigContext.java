package com.imagery.gui;

import com.imagery.model.ImageMetadata;

/**
 * State object holding temporary configuration when customizing a picture before placing it.
 */
public class ImageConfigContext {

    private final ImageMetadata metadata;
    private int width;
    private int height;
    private boolean lockRatio = true;
    private boolean invisible = true;
    private boolean glowing = false;

    public ImageConfigContext(ImageMetadata metadata, int defaultWidth, int defaultHeight) {
        this.metadata = metadata;
        this.width = defaultWidth;
        this.height = defaultHeight;
    }

    public ImageMetadata getMetadata() {
        return metadata;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = Math.max(1, Math.min(24, width));
        if (lockRatio) {
            updateHeightFromRatio();
        }
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = Math.max(1, Math.min(24, height));
        if (lockRatio) {
            updateWidthFromRatio();
        }
    }

    public boolean isLockRatio() {
        return lockRatio;
    }

    public void toggleLockRatio() {
        this.lockRatio = !this.lockRatio;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void toggleInvisible() {
        this.invisible = !this.invisible;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void toggleGlowing() {
        this.glowing = !this.glowing;
    }

    private void updateHeightFromRatio() {
        double ratio = metadata.getAspectRatio();
        if (ratio > 0) {
            this.height = Math.max(1, Math.min(24, (int) Math.round(this.width / ratio)));
        }
    }

    private void updateWidthFromRatio() {
        double ratio = metadata.getAspectRatio();
        if (ratio > 0) {
            this.width = Math.max(1, Math.min(24, (int) Math.round(this.height * ratio)));
        }
    }
}

