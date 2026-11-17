package com.imagery.gui;

import com.imagery.ImageryPlugin;
import com.imagery.model.ClickCommand;
import com.imagery.model.PlacedImage;
import com.imagery.util.ColorUtil;
import com.imagery.util.SoundUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles in-world player interactions with placed images (clicking to change pages,
 * triggering click commands, and preventing accidental frame rotation or destruction).
 */
public class WorldInteractionListener implements Listener {

    private final ImageryPlugin plugin;
    private final Map<UUID, Long> clickCooldown = new ConcurrentHashMap<>();

    public WorldInteractionListener(ImageryPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractFrame(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ItemFrame frame)) {
            return;
        }

        PlacedImage placed = plugin.getPlacedImageManager().getPlacedImageByFrame(frame.getUniqueId());
        if (placed == null) {
            return;
        }

        // Always prevent rotating the map inside the frame
        event.setCancelled(true);

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = clickCooldown.get(player.getUniqueId());
        if (last != null && (now - last) < 250) {
            return; // Debounce rapid clicking
        }
        clickCooldown.put(player.getUniqueId(), now);

        boolean hasMultiplePages = placed.getPages().size() > 1;

        // Shift-click: Previous page (if multi-page)
        if (player.isSneaking()) {
            if (hasMultiplePages) {
                plugin.getPlacedImageManager().prevPage(placed);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.9f, 1.1f);
                sendPageActionBar(player, placed);
            }
            executeCommands(player, placed);
            return;
        }

        // Normal right-click: Next page
        if (hasMultiplePages && placed.isClickToNextPage()) {
            plugin.getPlacedImageManager().nextPage(placed);
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.9f, 1.2f);
            sendPageActionBar(player, placed);
        }

        // Execute attached commands
        executeCommands(player, placed);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemFrame frame)) {
            return;
        }

        PlacedImage placed = plugin.getPlacedImageManager().getPlacedImageByFrame(frame.getUniqueId());
        if (placed == null) {
            return;
        }

        // Protect frame from taking damage or dropping item
        event.setCancelled(true);

        if (event.getDamager() instanceof Player player) {
            long now = System.currentTimeMillis();
            Long last = clickCooldown.get(player.getUniqueId());
            if (last != null && (now - last) < 300) {
                return;
            }
            clickCooldown.put(player.getUniqueId(), now);

            // Left-click can also trigger commands
            if (!placed.getClickCommands().isEmpty()) {
                executeCommands(player, placed);
            }
        }
    }

    private void executeCommands(Player player, PlacedImage placed) {
        if (placed.getClickCommands().isEmpty()) return;

        SoundUtil.playClick(player);
        for (ClickCommand cmd : placed.getClickCommands()) {
            cmd.execute(player, placed);
        }
    }

    private void sendPageActionBar(Player player, PlacedImage placed) {
        int current = placed.getCurrentPage() + 1;
        int total = placed.getPages().size();
        String file = placed.getCurrentFileName();

        // Extract just the simple filename for clean display
        if (file.contains("/")) {
            file = file.substring(file.lastIndexOf('/') + 1);
        }

        String bar = ColorUtil.color(
                "&#00f2fe📖 หน้า " + current + "/" + total + " &7(&f" + file + "&7) " +
                "&#7f8c8d[&#00f5a0คลิก: หน้าถัดไป &#7f8c8d| &#f6d365Shift+คลิก: หน้าก่อนหน้า&#7f8c8d]"
        );
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
    }
}

