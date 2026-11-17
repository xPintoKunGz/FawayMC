package com.bloomerymc.bloomeryx.listeners;

import com.bloomerymc.bloomeryx.BloomeryX;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerActivityListener implements Listener {

    private final BloomeryX plugin;

    public PlayerActivityListener(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Check if player actually moved positions (not just mouse yaw/pitch look)
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            plugin.getAfkManager().updateActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        plugin.getAfkManager().updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        // Don't wake from AFK if running /afk command itself
        String msg = event.getMessage().toLowerCase();
        if (!msg.startsWith("/afk")) {
            plugin.getAfkManager().updateActivity(event.getPlayer());
        }
    }
}

