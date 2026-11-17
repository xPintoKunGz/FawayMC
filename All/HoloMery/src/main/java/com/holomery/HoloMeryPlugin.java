package com.holomery;

import com.holomery.command.HoloCommand;
import com.holomery.gui.ChatInputManager;
import com.holomery.gui.HoloGuiListener;
import com.holomery.integration.PapiHook;
import com.holomery.listener.HologramListener;
import com.holomery.manager.HologramManager;
import com.holomery.manager.ThemeManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * HoloMery - Next-generation modern floating hologram plugin for Minecraft.
 * Features TextDisplay, ItemDisplay, BlockDisplay, Mob entities, interactive GUI editor,
 * multi-page click-to-flip navigation, and rich RGB gradient palettes.
 */
public final class HoloMeryPlugin extends JavaPlugin {

    private static HoloMeryPlugin instance;
    private ThemeManager themeManager;
    private HologramManager hologramManager;

    public static HoloMeryPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfig();

        // Initialize Theme Manager & load gradients
        themeManager = new ThemeManager(this);
        themeManager.loadThemes();

        // Initialize PlaceholderAPI hook
        PapiHook.init();
        if (PapiHook.isHooked()) {
            getLogger().info("PlaceholderAPI hooked successfully! Holograms support placeholders.");
        }

        // Initialize Hologram Manager & load entities
        hologramManager = new HologramManager(this);
        hologramManager.initialize();

        // Register Command & Tab Completer
        HoloCommand holoCommand = new HoloCommand(this);
        PluginCommand cmd = getCommand("holomery");
        if (cmd != null) {
            cmd.setExecutor(holoCommand);
            cmd.setTabCompleter(holoCommand);
        }

        // Register Events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new HologramListener(hologramManager), this);
        pm.registerEvents(new HoloGuiListener(), this);
        pm.registerEvents(new ChatInputManager(), this);

        // Register BungeeCord plugin messaging channel for server jumps
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Print modern startup message
        getLogger().info("========================================");
        getLogger().info("  HoloMery v" + getDescription().getVersion() + " by DeepMind Antigravity");
        getLogger().info("  Display Entities: Text, Item, Block & Mob Enabled!");
        getLogger().info("  Interactive In-Game GUI & Multi-Page Flipping Enabled!");
        getLogger().info("  Loaded Holograms: " + hologramManager.getAllHolograms().size());
        getLogger().info("  Loaded Themes: " + themeManager.getThemeNames().size());
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) {
            hologramManager.shutdown();
        }
        getLogger().info("HoloMery has been disabled cleanly. All display entities safely stored.");
        instance = null;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }
}
