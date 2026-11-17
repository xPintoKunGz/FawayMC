package com.antigravity.reversecrouch.listener;

import com.antigravity.reversecrouch.ReverseCrouch;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SneakSpeedListener implements Listener {

    private final ReverseCrouch plugin;
    private static final double VANILLA_DEFAULT_SNEAK_SPEED = 0.3;

    public SneakSpeedListener(ReverseCrouch plugin) {
        this.plugin = plugin;
    }

    public void applySpeed(Player player) {
        if (!plugin.getConfig().getBoolean("enabled", true)) {
            resetSpeed(player);
            return;
        }

        if (!plugin.getConfig().getBoolean("sneak-speed.modify-speed", true)) {
            resetSpeed(player);
            return;
        }

        if (player.hasPermission("reversecrouch.bypass")) {
            resetSpeed(player);
            return;
        }

        try {
            AttributeInstance attribute = player.getAttribute(Attribute.SNEAKING_SPEED);
            if (attribute != null) {
                double speed = plugin.getConfig().getDouble("sneak-speed.speed-value", 0.55);
                attribute.setBaseValue(speed);
            }
        } catch (Throwable ignored) {
            // Attribute might not exist on older unsupported versions
        }
    }

    public void resetSpeed(Player player) {
        try {
            AttributeInstance attribute = player.getAttribute(Attribute.SNEAKING_SPEED);
            if (attribute != null) {
                attribute.setBaseValue(VANILLA_DEFAULT_SNEAK_SPEED);
            }
        } catch (Throwable ignored) {
        }
    }

    public void applyToAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applySpeed(player);
        }
    }

    public void resetAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetSpeed(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        applySpeed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        resetSpeed(event.getPlayer());
    }
}

