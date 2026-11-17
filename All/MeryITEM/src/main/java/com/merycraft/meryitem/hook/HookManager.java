package com.merycraft.meryitem.hook;

import com.merycraft.meryitem.MeryItemPlugin;
import org.bukkit.Bukkit;

/**
 * Manages integrations with external plugins (PlaceholderAPI, Vault, etc.).
 */
public class HookManager {

    private final MeryItemPlugin plugin;
    private boolean placeholderAPIAvailable = false;

    public HookManager(MeryItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // PlaceholderAPI check
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new PlaceholderAPIHook(plugin).register();
                placeholderAPIAvailable = true;
                plugin.getLogger().info("Hooked into PlaceholderAPI successfully.");
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to register PlaceholderAPI expansion: " + t.getMessage());
            }
        } else {
            plugin.getLogger().info("PlaceholderAPI not found. Running in standalone mode.");
        }
    }

    public boolean isPlaceholderAPIAvailable() {
        return placeholderAPIAvailable;
    }
}

