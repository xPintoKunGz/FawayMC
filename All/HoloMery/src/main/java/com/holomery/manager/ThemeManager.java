package com.holomery.manager;

import com.holomery.HoloMeryPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages custom color themes and gradients for HoloMery.
 */
public class ThemeManager {

    private static ThemeManager instance;
    private final HoloMeryPlugin plugin;
    private final Map<String, List<Color>> themes = new LinkedHashMap<>();
    private File themesFile;
    private FileConfiguration themesConfig;

    public ThemeManager(HoloMeryPlugin plugin) {
        instance = this;
        this.plugin = plugin;
    }

    public static ThemeManager getInstance() {
        return instance;
    }

    public void loadThemes() {
        themes.clear();

        themesFile = new File(plugin.getDataFolder(), "themes.yml");
        if (!themesFile.exists()) {
            plugin.saveResource("themes.yml", false);
        }

        themesConfig = YamlConfiguration.loadConfiguration(themesFile);
        ConfigurationSection section = themesConfig.getConfigurationSection("themes");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                List<String> hexList = section.getStringList(key);
                List<Color> colors = new ArrayList<>();
                for (String hex : hexList) {
                    hex = hex.trim();
                    if (hex.startsWith("#")) hex = hex.substring(1);
                    if (hex.length() == 6) {
                        try {
                            colors.add(new Color(Integer.parseInt(hex, 16)));
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Invalid hex color in theme '" + key + "': " + hex);
                        }
                    }
                }
                if (!colors.isEmpty()) {
                    themes.put(key.toLowerCase(), colors);
                }
            }
        }

        // Register default fallback themes if empty
        if (themes.isEmpty()) {
            registerDefaults();
        }

        plugin.getLogger().info("Loaded " + themes.size() + " holographic color themes.");
    }

    private void registerDefaults() {
        registerTheme("cyberpunk", List.of("#00F2FE", "#4FACFE", "#FF0080"));
        registerTheme("aurora", List.of("#00F5A0", "#00D9F5", "#7A00FF"));
        registerTheme("sunset", List.of("#FA709A", "#FEE140", "#9A1F6E"));
        registerTheme("neon", List.of("#00FF87", "#60EFFF"));
        registerTheme("sakura", List.of("#FF9A9E", "#FECFEF", "#F64F59"));
        registerTheme("amethyst", List.of("#8A2387", "#E94057", "#F27121"));
        registerTheme("galaxy", List.of("#120E43", "#5F27CD", "#C06C84"));
        registerTheme("ocean", List.of("#2E3192", "#1BFFFF"));
        registerTheme("fire", List.of("#FF0844", "#FFB199", "#FFDE59"));
        registerTheme("ice", List.of("#A1C4FD", "#C2E9FB", "#00C9FF"));
        registerTheme("rainbow", List.of("#FF0000", "#FF7F00", "#FFFF00", "#00FF00", "#0000FF", "#4B0082", "#9400D3"));
        registerTheme("gold", List.of("#F7971E", "#FFD200"));
        registerTheme("synthwave", List.of("#FC466B", "#3F5EFB"));
        registerTheme("matrix", List.of("#00FF66", "#008822"));
        registerTheme("candy", List.of("#FF99C8", "#FCF6BD", "#D0F4DE", "#A9DEF9"));
    }

    private void registerTheme(String name, List<String> hexCodes) {
        List<Color> colors = new ArrayList<>();
        for (String hex : hexCodes) {
            if (hex.startsWith("#")) hex = hex.substring(1);
            try {
                colors.add(new Color(Integer.parseInt(hex, 16)));
            } catch (Exception ignored) {}
        }
        themes.put(name.toLowerCase(), colors);
    }

    public List<Color> getThemeColors(String themeName) {
        return themes.get(themeName.toLowerCase());
    }

    public boolean hasTheme(String themeName) {
        return themes.containsKey(themeName.toLowerCase());
    }

    public Set<String> getThemeNames() {
        return themes.keySet();
    }

    public void addCustomTheme(String name, List<String> hexCodes) {
        List<Color> colors = new ArrayList<>();
        List<String> validHexes = new ArrayList<>();
        for (String hex : hexCodes) {
            String clean = hex.trim();
            if (clean.startsWith("#")) clean = clean.substring(1);
            if (clean.length() == 6) {
                try {
                    colors.add(new Color(Integer.parseInt(clean, 16)));
                    validHexes.add("#" + clean.toUpperCase());
                } catch (Exception ignored) {}
            }
        }
        if (!colors.isEmpty()) {
            themes.put(name.toLowerCase(), colors);
            themesConfig.set("themes." + name.toLowerCase(), validHexes);
            try {
                themesConfig.save(themesFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save custom theme to themes.yml", e);
            }
        }
    }
}

