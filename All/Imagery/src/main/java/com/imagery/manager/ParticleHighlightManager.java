package com.imagery.manager;

import com.imagery.ImageryPlugin;
import com.imagery.model.PlacedImage;
import com.imagery.util.ColorUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Highlights placed images in the Minecraft world with Style 26.3 glowing boundary particles
 * so players can visually locate their pictures through walls and across distances.
 */
public class ParticleHighlightManager {

    private final ImageryPlugin plugin;
    private final Map<UUID, BukkitTask> activeHighlights = new ConcurrentHashMap<>();

    public ParticleHighlightManager(ImageryPlugin plugin) {
        this.plugin = plugin;
    }

    public void highlightImage(Player player, PlacedImage image, int durationSeconds) {
        if (player == null || image == null) return;

        // Cancel previous highlight task for this player if any
        BukkitTask prev = activeHighlights.remove(player.getUniqueId());
        if (prev != null) {
            prev.cancel();
        }

        World world = image.getWorld();
        if (world == null) return;

        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0, 242, 254), 1.6f);

        BukkitTask task = new BukkitRunnable() {
            int ticksLeft = durationSeconds * 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticksLeft <= 0) {
                    cancel();
                    activeHighlights.remove(player.getUniqueId());
                    return;
                }

                // Render boundary box every 4 ticks
                if (ticksLeft % 4 == 0) {
                    drawBoundaryBox(player, image, dust);
                }

                // Action bar reminder every second
                if (ticksLeft % 20 == 0) {
                    int seconds = ticksLeft / 20;
                    String bar = ColorUtil.color(
                            "&#00f2fe📍 &#4facfeViewing: &f" + image.getFileName() +
                            " &#7f8c8d| &#00f5a0[X: " + image.getOriginX() + ", Y: " + image.getOriginY() + ", Z: " + image.getOriginZ() + "] " +
                            "&#7f8c8d(&#ffb199" + seconds + "s left&7)"
                    );
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
                }

                ticksLeft--;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeHighlights.put(player.getUniqueId(), task);
    }

    private void drawBoundaryBox(Player player, PlacedImage image, Particle.DustOptions dust) {
        World world = image.getWorld();
        if (world == null) return;

        int w = image.getWidth();
        int h = image.getHeight();

        Location topLeft = image.getTileLocation(0, 0);
        Location topRight = image.getTileLocation(0, w - 1);
        Location bottomLeft = image.getTileLocation(h - 1, 0);
        Location bottomRight = image.getTileLocation(h - 1, w - 1);

        if (topLeft == null || topRight == null || bottomLeft == null || bottomRight == null) return;

        // Draw edges of the image surface
        drawParticleLine(player, topLeft, topRight, dust);
        drawParticleLine(player, topRight, bottomRight, dust);
        drawParticleLine(player, bottomRight, bottomLeft, dust);
        drawParticleLine(player, bottomLeft, topLeft, dust);

        // Center beacon pulse
        Location center = image.getCenterLocation();
        if (center != null) {
            player.spawnParticle(Particle.GLOW, center, 4, 0.2, 0.2, 0.2, 0.02);
            player.spawnParticle(Particle.END_ROD, center, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private void drawParticleLine(Player player, Location from, Location to, Particle.DustOptions dust) {
        Location start = from.clone().add(0.5, 0.5, 0.5);
        Location end = to.clone().add(0.5, 0.5, 0.5);

        double distance = start.distance(end);
        int points = Math.max(2, (int) (distance * 3));

        double dx = (end.getX() - start.getX()) / points;
        double dy = (end.getY() - start.getY()) / points;
        double dz = (end.getZ() - start.getZ()) / points;

        Location current = start.clone();
        for (int i = 0; i <= points; i++) {
            player.spawnParticle(Particle.REDSTONE, current, 1, dust);
            current.add(dx, dy, dz);
        }
    }

    public void cancelAll() {
        for (BukkitTask task : activeHighlights.values()) {
            task.cancel();
        }
        activeHighlights.clear();
    }
}
