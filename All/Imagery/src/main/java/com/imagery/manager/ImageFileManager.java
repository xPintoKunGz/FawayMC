package com.imagery.manager;

import com.imagery.ImageryPlugin;
import com.imagery.model.ImageMetadata;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles scanning, indexing, and loading of image files inside the plugins/Imagery/images directory.
 */
public class ImageFileManager {

    private final ImageryPlugin plugin;
    private final File imagesFolder;
    private final Map<String, ImageMetadata> metadataCache = new ConcurrentHashMap<>();
    private final Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();

    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(
            Arrays.asList("png", "jpg", "jpeg", "bmp", "gif")
    );

    public ImageFileManager(ImageryPlugin plugin) {
        this.plugin = plugin;
        this.imagesFolder = new File(plugin.getDataFolder(), "images");
    }

    public void init() {
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs();
            createDefaultWelcomeImage();
        }
        scanImages();
    }

    /**
     * Rescans the images directory recursively and updates metadata cache.
     */
    public synchronized void scanImages() {
        metadataCache.clear();
        imageCache.clear();

        scanDirectoryRecursive(imagesFolder, "");

        plugin.getLogger().info("Scanned " + metadataCache.size() + " image(s) from " + imagesFolder.getPath());
    }

    private void scanDirectoryRecursive(File dir, String prefix) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String subPrefix = prefix.isEmpty() ? file.getName() : prefix + "/" + file.getName();
                scanDirectoryRecursive(file, subPrefix);
                continue;
            }

            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex <= 0) continue;

            String ext = name.substring(dotIndex + 1).toLowerCase();
            if (!SUPPORTED_EXTENSIONS.contains(ext)) continue;

            String relativeKey = prefix.isEmpty() ? name : prefix + "/" + name;

            try {
                BufferedImage bimg = ImageIO.read(file);
                if (bimg != null) {
                    ImageMetadata meta = new ImageMetadata(relativeKey, file, bimg.getWidth(), bimg.getHeight(), file.length());
                    metadataCache.put(relativeKey.toLowerCase(), meta);
                    imageCache.put(relativeKey.toLowerCase(), bimg);

                    // Also index by bare filename if not already taken
                    if (!metadataCache.containsKey(name.toLowerCase())) {
                        metadataCache.put(name.toLowerCase(), meta);
                        imageCache.put(name.toLowerCase(), bimg);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to read image file: " + file.getName(), e);
            }
        }
    }

    public File getImagesFolder() {
        return imagesFolder;
    }

    public Collection<ImageMetadata> getAllMetadata() {
        return metadataCache.values();
    }

    public ImageMetadata getMetadata(String fileName) {
        if (fileName == null) return null;
        ImageMetadata meta = metadataCache.get(fileName.toLowerCase());
        if (meta == null) {
            // Try with extension if user omitted it
            for (String ext : SUPPORTED_EXTENSIONS) {
                meta = metadataCache.get((fileName + "." + ext).toLowerCase());
                if (meta != null) break;
            }
        }
        return meta;
    }

    public BufferedImage getImage(String fileName) {
        if (fileName == null) return null;
        String key = fileName.toLowerCase();
        BufferedImage img = imageCache.get(key);
        if (img != null) return img;

        ImageMetadata meta = getMetadata(fileName);
        if (meta != null && meta.getFile().exists()) {
            try {
                img = ImageIO.read(meta.getFile());
                if (img != null) {
                    imageCache.put(key, img);
                    return img;
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error loading image: " + fileName, e);
            }
        }
        return null;
    }

    /**
     * Creates an aesthetic default Style 26.3 welcome image when folder is first created.
     */
    private void createDefaultWelcomeImage() {
        File defaultFile = new File(imagesFolder, "welcome.png");
        if (defaultFile.exists()) return;

        try {
            int w = 512;
            int h = 512;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Modern gradient background
            GradientPaint gradient = new GradientPaint(0, 0, new Color(15, 23, 42), w, h, new Color(30, 41, 59));
            g.setPaint(gradient);
            g.fillRect(0, 0, w, h);

            // Glowing border
            g.setColor(new Color(0, 242, 254, 180));
            g.setStroke(new BasicStroke(8f));
            g.drawRoundRect(16, 16, w - 32, h - 32, 24, 24);

            // Inner cyan grid lines
            g.setColor(new Color(0, 242, 254, 40));
            g.setStroke(new BasicStroke(1.5f));
            for (int x = 40; x < w - 40; x += 32) {
                g.drawLine(x, 40, x, h - 40);
            }
            for (int y = 40; y < h - 40; y += 32) {
                g.drawLine(40, y, w - 40, y);
            }

            // Central logo glow
            g.setColor(new Color(79, 172, 254, 50));
            g.fillOval(w / 2 - 120, 110, 240, 240);

            // Diamond badge
            int cx = w / 2;
            int cy = 200;
            g.setColor(new Color(0, 242, 254));
            Polygon diamond = new Polygon();
            diamond.addPoint(cx, cy - 50);
            diamond.addPoint(cx + 50, cy);
            diamond.addPoint(cx, cy + 50);
            diamond.addPoint(cx - 50, cy);
            g.fillPolygon(diamond);

            g.setColor(new Color(255, 255, 255));
            Polygon innerDiamond = new Polygon();
            innerDiamond.addPoint(cx, cy - 25);
            innerDiamond.addPoint(cx + 25, cy);
            innerDiamond.addPoint(cx, cy + 25);
            innerDiamond.addPoint(cx - 25, cy);
            g.fillPolygon(innerDiamond);

            // Title
            g.setFont(new Font("SansSerif", Font.BOLD, 42));
            g.setColor(new Color(255, 255, 255));
            FontMetrics fm = g.getFontMetrics();
            String title = "IMAGERY";
            g.drawString(title, (w - fm.stringWidth(title)) / 2, 330);

            // Subtitle
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.setColor(new Color(0, 245, 160));
            fm = g.getFontMetrics();
            String sub = "STYLE 26.3 SYSTEM";
            g.drawString(sub, (w - fm.stringWidth(sub)) / 2, 370);

            // Description
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(161, 140, 209));
            fm = g.getFontMetrics();
            String note = "Drop your images into plugins/Imagery/images/";
            g.drawString(note, (w - fm.stringWidth(note)) / 2, 420);

            g.dispose();
            ImageIO.write(img, "png", defaultFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not generate sample welcome.png image", e);
        }
    }
}

