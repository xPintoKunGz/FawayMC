package com.imagery.manager;

import com.imagery.ImageryPlugin;
import com.imagery.model.ImageAlbum;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages photo albums and multi-page sets from folders and albums.yml.
 */
public class AlbumManager {

    private final ImageryPlugin plugin;
    private final File albumsDir;
    private final File albumsFile;
    private final Map<String, ImageAlbum> albums = new ConcurrentHashMap<>();

    public AlbumManager(ImageryPlugin plugin) {
        this.plugin = plugin;
        this.albumsDir = new File(plugin.getImageFileManager().getImagesFolder(), "albums");
        this.albumsFile = new File(plugin.getDataFolder(), "albums.yml");
    }

    public void init() {
        if (!albumsDir.exists()) {
            albumsDir.mkdirs();
            createSampleAlbum();
        }
        loadAlbums();
    }

    public synchronized void loadAlbums() {
        albums.clear();

        // 1. Scan albums folder (each subfolder is an album)
        File[] folders = albumsDir.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                String albumId = folder.getName().toLowerCase();
                List<String> pages = new ArrayList<>();

                File[] files = folder.listFiles((dir, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".png") || lower.endsWith(".jpg") ||
                            lower.endsWith(".jpeg") || lower.endsWith(".bmp") || lower.endsWith(".gif");
                });

                if (files != null) {
                    Arrays.sort(files, Comparator.comparing(File::getName));
                    for (File f : files) {
                        // Store relative name e.g. "albums/rules/01.png"
                        String rel = "albums/" + folder.getName() + "/" + f.getName();
                        pages.add(rel);
                    }
                }

                ImageAlbum album = new ImageAlbum(albumId, folder.getName(), pages, 0);
                albums.put(albumId, album);
            }
        }

        // 2. Load from albums.yml (custom configured albums)
        if (albumsFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(albumsFile);
            if (yaml.contains("albums")) {
                for (String key : yaml.getConfigurationSection("albums").getKeys(false)) {
                    try {
                        Map<String, Object> values = yaml.getConfigurationSection("albums." + key).getValues(true);
                        ImageAlbum album = ImageAlbum.deserialize(values);
                        albums.put(album.getId().toLowerCase(), album);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to load album: " + key, e);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + albums.size() + " album(s).");
    }

    public void saveAlbums() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (ImageAlbum album : albums.values()) {
            yaml.set("albums." + album.getId(), album.serialize());
        }
        try {
            yaml.save(albumsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save albums.yml", e);
        }
    }

    public Collection<ImageAlbum> getAllAlbums() {
        return albums.values();
    }

    public ImageAlbum getAlbum(String id) {
        if (id == null) return null;
        return albums.get(id.toLowerCase());
    }

    public void registerAlbum(ImageAlbum album) {
        if (album != null) {
            albums.put(album.getId().toLowerCase(), album);
            saveAlbums();
        }
    }

    public void deleteAlbum(String id) {
        if (id != null) {
            albums.remove(id.toLowerCase());
            saveAlbums();
        }
    }

    /**
     * Creates a demo multi-page album with 3 styled cards.
     */
    private void createSampleAlbum() {
        File demoDir = new File(albumsDir, "demo_album");
        if (demoDir.exists()) return;
        demoDir.mkdirs();

        createSamplePage(new File(demoDir, "page1_intro.png"), "หน้า 1/3: ยินดีต้อนรับ", "คลิกขวาที่ภาพเพื่อเปิดหน้าถัดไป!", new Color(0, 242, 254));
        createSamplePage(new File(demoDir, "page2_features.png"), "หน้า 2/3: สไลด์โชว์", "รองรับเซ็ตภาพและการหมุนสไลด์", new Color(0, 245, 160));
        createSamplePage(new File(demoDir, "page3_commands.png"), "หน้า 3/3: คำสั่งคลิก", "คลิกเพื่อสั่งการคำสั่งในเกม!", new Color(161, 140, 209));
    }

    private void createSamplePage(File file, String title, String subtitle, Color accent) {
        try {
            int w = 512;
            int h = 512;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Dark sleek background
            g.setColor(new Color(15, 23, 42));
            g.fillRect(0, 0, w, h);

            // Frame
            g.setColor(accent);
            g.setStroke(new BasicStroke(6f));
            g.drawRoundRect(16, 16, w - 32, h - 32, 20, 20);

            // Glowing circle
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
            g.fillOval(w / 2 - 100, 100, 200, 200);

            // Title
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, (w - fm.stringWidth(title)) / 2, 220);

            // Subtitle
            g.setFont(new Font("SansSerif", Font.PLAIN, 22));
            g.setColor(accent);
            fm = g.getFontMetrics();
            g.drawString(subtitle, (w - fm.stringWidth(subtitle)) / 2, 280);

            // Hint
            g.setFont(new Font("SansSerif", Font.ITALIC, 16));
            g.setColor(new Color(200, 200, 200));
            String hint = "Imagery Multi-Page Album System";
            fm = g.getFontMetrics();
            g.drawString(hint, (w - fm.stringWidth(hint)) / 2, 420);

            g.dispose();
            ImageIO.write(img, "png", file);
        } catch (Exception ignored) {}
    }
}
