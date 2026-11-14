package org.kcm.bloomery.fawaymc.global;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ItemsConfigManager {

    private final JavaPlugin plugin;
    private File itemsFile;
    private FileConfiguration itemsConfig;

    public ItemsConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createConfig();
    }

    public void createConfig() {
        itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);

        try (InputStreamReader reader = new InputStreamReader(
                plugin.getResource("items.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            itemsConfig.setDefaults(defConfig);
        } catch (Exception ignored) {}

        plugin.getLogger().info("[Items] config loaded | gui="
                + itemsConfig.getBoolean("gui.open_item.enabled", true)
                + " | profile=" + itemsConfig.getBoolean("profile.item.enabled", true)
                + " | visibility=" + itemsConfig.getBoolean("visibility.item.enabled", true));
    }

    public FileConfiguration getConfig() {
        return itemsConfig;
    }

    public void reload() {
        if (itemsFile == null) itemsFile = new File(plugin.getDataFolder(), "items.yml");
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);

        try (InputStreamReader reader = new InputStreamReader(
                plugin.getResource("items.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            itemsConfig.setDefaults(defConfig);
        } catch (Exception ignored) {}
    }

    public void save() {
        if (itemsConfig == null || itemsFile == null) return;
        try {
            itemsConfig.save(itemsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถบันทึก items.yml ได้: " + e.getMessage());
        }
    }
}