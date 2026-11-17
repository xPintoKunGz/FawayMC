package com.imagery.renderer;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance Bukkit MapRenderer that renders a cached 128x128 image tile
 * onto a Minecraft MapCanvas without resource packs.
 */
public class CustomImageRenderer extends MapRenderer {

    private BufferedImage imageTile;
    private final Set<UUID> renderedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public CustomImageRenderer(BufferedImage imageTile) {
        super(true); // Contextual per player
        this.imageTile = imageTile;
    }

    public synchronized void setImageTile(BufferedImage newTile) {
        this.imageTile = newTile;
        invalidateAll();
    }

    public BufferedImage getImageTile() {
        return imageTile;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (player == null || imageTile == null) {
            return;
        }

        // Render once per player session to minimize server workload
        if (renderedPlayers.add(player.getUniqueId())) {
            canvas.drawImage(0, 0, imageTile);
        }
    }

    /**
     * Resets the render cache for a specific player (e.g. when teleporting or reloading)
     */
    public void invalidate(Player player) {
        if (player != null) {
            renderedPlayers.remove(player.getUniqueId());
        }
    }

    /**
     * Resets render cache for all players
     */
    public void invalidateAll() {
        renderedPlayers.clear();
    }
}

