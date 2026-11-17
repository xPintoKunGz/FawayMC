package com.imagery.manager;

import com.imagery.ImageryPlugin;
import com.imagery.model.ImageAlbum;
import com.imagery.model.ImageMetadata;
import com.imagery.model.PlacedImage;
import com.imagery.renderer.CustomImageRenderer;
import com.imagery.util.ColorUtil;
import com.imagery.util.ImageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages placed images in the Minecraft world, including spawning frames,
 * rendering sliced map canvases, resizing, repositioning, and YAML persistence.
 */
public class PlacedImageManager {

    private final ImageryPlugin plugin;
    private final File dataFile;
    private final Map<String, PlacedImage> placedImages = new ConcurrentHashMap<>();

    public PlacedImageManager(ImageryPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "placed_images.yml");
    }

    public void loadPlacedImages() {
        placedImages.clear();
        if (!dataFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        if (!yaml.contains("images")) return;

        for (String key : yaml.getConfigurationSection("images").getKeys(false)) {
            try {
                Map<String, Object> values = yaml.getConfigurationSection("images." + key).getValues(true);
                PlacedImage placed = PlacedImage.deserialize(values);
                placedImages.put(placed.getId(), placed);
                rebindMapRenderers(placed);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load placed image with id: " + key, e);
            }
        }

        plugin.getLogger().info("Loaded " + placedImages.size() + " placed image(s).");
        startAutoSlideshowTask();
    }

    public void savePlacedImages() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlacedImage img : placedImages.values()) {
            yaml.set("images." + img.getId(), img.serialize());
        }
        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save placed_images.yml", e);
        }
    }

    public Collection<PlacedImage> getAllPlacedImages() {
        return placedImages.values();
    }

    public PlacedImage getPlacedImage(String id) {
        if (id == null) return null;
        return placedImages.get(id);
    }

    /**
     * Finds a PlacedImage by one of its item frame UUIDs.
     */
    public PlacedImage getPlacedImageByFrame(UUID frameUuid) {
        if (frameUuid == null) return null;
        for (PlacedImage img : placedImages.values()) {
            if (img.getFrameUuids().contains(frameUuid)) {
                return img;
            }
        }
        return null;
    }

    /**
     * Changes page for an album or multi-page image, updating map tiles on the fly.
     */
    public boolean setPage(PlacedImage placed, int pageIndex) {
        if (placed == null || placed.getPages().isEmpty()) return false;

        placed.setCurrentPage(pageIndex);
        String targetFile = placed.getCurrentFileName();
        BufferedImage raw = plugin.getImageFileManager().getImage(targetFile);
        if (raw == null) return false;

        BufferedImage scaled = ImageUtil.resize(raw, placed.getWidth(), placed.getHeight());
        BufferedImage[][] tiles = ImageUtil.slice(scaled, placed.getWidth(), placed.getHeight());

        int index = 0;
        for (int r = 0; r < placed.getHeight(); r++) {
            for (int c = 0; c < placed.getWidth(); c++) {
                if (index < placed.getMapIds().size()) {
                    int mapId = placed.getMapIds().get(index);
                    MapView mapView = Bukkit.getMap(mapId);
                    if (mapView != null) {
                        boolean found = false;
                        for (org.bukkit.map.MapRenderer renderer : mapView.getRenderers()) {
                            if (renderer instanceof CustomImageRenderer cir) {
                                cir.setImageTile(tiles[r][c]);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            mapView.getRenderers().forEach(mapView::removeRenderer);
                            mapView.addRenderer(new CustomImageRenderer(tiles[r][c]));
                        }
                    }
                }
                index++;
            }
        }

        // Force send map update packet to nearby players so it changes instantly
        World world = placed.getWorld();
        Location center = placed.getCenterLocation();
        if (world != null && center != null) {
            for (Player p : world.getPlayers()) {
                if (p.getLocation().distanceSquared(center) <= 64 * 64) {
                    for (int mapId : placed.getMapIds()) {
                        MapView v = Bukkit.getMap(mapId);
                        if (v != null) {
                            p.sendMap(v);
                        }
                    }
                }
            }
        }

        return true;
    }

    public boolean nextPage(PlacedImage placed) {
        if (placed == null || placed.getPages().size() <= 1) return false;
        int next = placed.nextPage();
        return setPage(placed, next);
    }

    public boolean prevPage(PlacedImage placed) {
        if (placed == null || placed.getPages().size() <= 1) return false;
        int prev = placed.prevPage();
        return setPage(placed, prev);
    }

    /**
     * Starts background task to rotate slideshow albums.
     */
    private org.bukkit.scheduler.BukkitTask slideshowTask;

    public void startAutoSlideshowTask() {
        if (slideshowTask != null) {
            slideshowTask.cancel();
        }

        slideshowTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentSec = System.currentTimeMillis() / 1000L;
            for (PlacedImage img : placedImages.values()) {
                if (img.getAutoRotateSeconds() > 0 && img.getPages().size() > 1) {
                    if (currentSec % img.getAutoRotateSeconds() == 0) {
                        nextPage(img);
                    }
                }
            }
        }, 20L, 20L);
    }

    public void stopAutoSlideshowTask() {
        if (slideshowTask != null) {
            slideshowTask.cancel();
            slideshowTask = null;
        }
    }

    /**
     * Reattaches CustomImageRenderers to existing map views when server reloads/starts.
     */
    public void rebindMapRenderers(PlacedImage placed) {
        BufferedImage source = plugin.getImageFileManager().getImage(placed.getFileName());
        if (source == null) return;

        BufferedImage scaled = ImageUtil.resize(source, placed.getWidth(), placed.getHeight());
        BufferedImage[][] tiles = ImageUtil.slice(scaled, placed.getWidth(), placed.getHeight());

        int index = 0;
        for (int r = 0; r < placed.getHeight(); r++) {
            for (int c = 0; c < placed.getWidth(); c++) {
                if (index < placed.getMapIds().size()) {
                    int mapId = placed.getMapIds().get(index);
                    MapView mapView = Bukkit.getMap(mapId);
                    if (mapView != null) {
                        mapView.getRenderers().forEach(mapView::removeRenderer);
                        mapView.addRenderer(new CustomImageRenderer(tiles[r][c]));
                        mapView.setTrackingPosition(false);
                    }
                }
                index++;
            }
        }
    }

    /**
     * Places a new image on the wall targeted by the player.
     */
    public PlacedImage placeImage(Player player, Block targetBlock, BlockFace clickedFace,
                                  ImageMetadata meta, int width, int height, boolean invisible, boolean glowing) {
        if (player == null || targetBlock == null || meta == null) return null;

        World world = targetBlock.getWorld();
        BufferedImage raw = plugin.getImageFileManager().getImage(meta.getFileName());
        if (raw == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่สามารถอ่านไฟล์รูปภาพ &e" + meta.getFileName() + " &cได้"));
            return null;
        }

        // Generate unique ID
        String id = "img_" + (System.currentTimeMillis() % 100000);
        int counter = 1;
        while (placedImages.containsKey(id)) {
            id = "img_" + ((System.currentTimeMillis() + counter++) % 100000);
        }

        // Determine anchor block based on face
        // We place frames against the clicked block face
        int anchorX = targetBlock.getX();
        int anchorY = targetBlock.getY() + height - 1; // Top block
        int anchorZ = targetBlock.getZ();

        PlacedImage placed = new PlacedImage(id, meta.getFileName(), world.getName(),
                anchorX, anchorY, anchorZ, clickedFace, width, height, invisible, glowing);

        // Resize and slice
        BufferedImage scaled = ImageUtil.resize(raw, width, height);
        BufferedImage[][] tiles = ImageUtil.slice(scaled, width, height);

        // Spawn frames and assign maps
        spawnFramesAndMaps(placed, tiles, world);

        placedImages.put(placed.getId(), placed);
        savePlacedImages();

        return placed;
    }

    /**
     * Places a multi-page album onto a wall.
     */
    public PlacedImage placeAlbum(Player player, Block targetBlock, BlockFace clickedFace,
                                  ImageAlbum album, int width, int height, boolean invisible, boolean glowing) {
        if (player == null || targetBlock == null || album == null || album.getPages().isEmpty()) return null;

        String firstFile = album.getPages().get(0);
        ImageMetadata meta = plugin.getImageFileManager().getMetadata(firstFile);
        if (meta == null) {
            meta = new ImageMetadata(firstFile, null, 512, 512, 0);
        }

        PlacedImage placed = placeImage(player, targetBlock, clickedFace, meta, width, height, invisible, glowing);
        if (placed != null) {
            placed.setAlbumId(album.getId());
            placed.setPages(album.getPages());
            placed.setAutoRotateSeconds(album.getAutoRotateSeconds());
            placed.setCurrentPage(0);
            savePlacedImages();
        }
        return placed;
    }

    /**
     * Resizes an existing placed image to new dimensions.
     */
    public boolean resizeImage(PlacedImage placed, int newWidth, int newHeight) {
        if (placed == null || newWidth <= 0 || newHeight <= 0) return false;

        World world = placed.getWorld();
        if (world == null) return false;

        BufferedImage raw = plugin.getImageFileManager().getImage(placed.getFileName());
        if (raw == null) return false;

        // Remove old frames
        clearFrames(placed);
        placed.getFrameUuids().clear();
        placed.getMapIds().clear();

        placed.setWidth(newWidth);
        placed.setHeight(newHeight);

        // Resize and slice with new dimensions
        BufferedImage scaled = ImageUtil.resize(raw, newWidth, newHeight);
        BufferedImage[][] tiles = ImageUtil.slice(scaled, newWidth, newHeight);

        // Respawn frames and maps
        spawnFramesAndMaps(placed, tiles, world);
        savePlacedImages();

        return true;
    }

    /**
     * Updates visibility or glowing properties on existing frames.
     */
    public void updateFrameProperties(PlacedImage placed) {
        World world = placed.getWorld();
        if (world == null) return;

        for (UUID uuid : placed.getFrameUuids()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof ItemFrame frame) {
                frame.setVisible(!placed.isInvisibleFrame());
            }
        }
        savePlacedImages();
    }

    /**
     * Removes a placed image completely from the world and database.
     */
    public boolean removeImage(PlacedImage placed) {
        if (placed == null) return false;

        clearFrames(placed);
        placedImages.remove(placed.getId());
        savePlacedImages();
        return true;
    }

    private void clearFrames(PlacedImage placed) {
        for (UUID uuid : placed.getFrameUuids()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private void spawnFramesAndMaps(PlacedImage placed, BufferedImage[][] tiles, World world) {
        int width = placed.getWidth();
        int height = placed.getHeight();
        BlockFace facing = placed.getFacing();

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                Location blockLoc = placed.getTileLocation(r, c);
                if (blockLoc == null) continue;

                // Frame location is slightly offset towards the facing direction
                Location frameLoc = blockLoc.clone().add(
                        facing.getModX(),
                        facing.getModY(),
                        facing.getModZ()
                );

                // Clean up any overlapping item frame
                for (Entity e : world.getNearbyEntities(frameLoc, 0.5, 0.5, 0.5)) {
                    if (e instanceof ItemFrame) {
                        e.remove();
                    }
                }

                // Create map view
                MapView mapView = Bukkit.createMap(world);
                mapView.getRenderers().forEach(mapView::removeRenderer);
                mapView.addRenderer(new CustomImageRenderer(tiles[r][c]));
                mapView.setTrackingPosition(false);
                mapView.setUnlimitedTracking(false);

                ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                MapMeta meta = (MapMeta) mapItem.getItemMeta();
                if (meta != null) {
                    meta.setMapView(mapView);
                    mapItem.setItemMeta(meta);
                }

                placed.getMapIds().add(mapView.getId());

                try {
                    Class<? extends ItemFrame> frameClass = placed.isGlowing() ? GlowItemFrame.class : ItemFrame.class;
                    ItemFrame frame = world.spawn(frameLoc, frameClass, f -> {
                        f.setFacingDirection(facing, true);
                        f.setVisible(!placed.isInvisibleFrame());
                        f.setFixed(true);
                        f.setItem(mapItem, false);
                    });

                    placed.getFrameUuids().add(frame.getUniqueId());
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Error spawning item frame at " + frameLoc, ex);
                }
            }
        }
    }
}

