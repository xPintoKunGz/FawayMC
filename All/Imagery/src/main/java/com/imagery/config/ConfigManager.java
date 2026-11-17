package com.imagery.config;

import com.imagery.ImageryPlugin;
import com.imagery.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages configuration values, limits, and customizable Style 26.3 messages.
 */
public class ConfigManager {

    private final ImageryPlugin plugin;
    private int maxBlockWidth;
    private int maxBlockHeight;
    private boolean defaultInvisibleFrames;
    private boolean defaultGlowing;
    private int highlightDurationSeconds;

    public ConfigManager(ImageryPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.maxBlockWidth = config.getInt("settings.max-width", 16);
        this.maxBlockHeight = config.getInt("settings.max-height", 16);
        this.defaultInvisibleFrames = config.getBoolean("settings.default-invisible-frames", true);
        this.defaultGlowing = config.getBoolean("settings.default-glowing", false);
        this.highlightDurationSeconds = config.getInt("settings.highlight-duration-seconds", 12);
    }

    public int getMaxBlockWidth() {
        return maxBlockWidth;
    }

    public int getMaxBlockHeight() {
        return maxBlockHeight;
    }

    public boolean isDefaultInvisibleFrames() {
        return defaultInvisibleFrames;
    }

    public boolean isDefaultGlowing() {
        return defaultGlowing;
    }

    public int getHighlightDurationSeconds() {
        return highlightDurationSeconds;
    }

    public String getMessage(String path, String def) {
        return ColorUtil.color(plugin.getConfig().getString("messages." + path, def));
    }
}

