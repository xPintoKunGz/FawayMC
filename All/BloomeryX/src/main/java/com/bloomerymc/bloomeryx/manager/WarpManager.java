package com.bloomerymc.bloomeryx.manager;

import com.bloomerymc.bloomeryx.BloomeryX;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WarpManager {

    private final BloomeryX plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<String, Location> warps = new HashMap<>();

    public WarpManager(BloomeryX plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
        load();
    }

    public void load() {
        warps.clear();
        if (!file.exists()) {
            return;
        }
        config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            Location loc = PlayerDataManager.loadLocation(config.getConfigurationSection(key));
            if (loc != null) {
                warps.put(key.toLowerCase(), loc);
            }
        }
    }

    public void setWarp(String name, Location location) {
        warps.put(name.toLowerCase(), location);
        if (config == null) {
            config = new YamlConfiguration();
        }
        PlayerDataManager.saveLocation(config.createSection(name.toLowerCase()), location);
        save();
    }

    public boolean deleteWarp(String name) {
        if (warps.remove(name.toLowerCase()) != null) {
            if (config != null) {
                config.set(name.toLowerCase(), null);
                save();
            }
            return true;
        }
        return false;
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public Set<String> getWarpNames() {
        return Collections.unmodifiableSet(warps.keySet());
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save warps to warps.yml");
        }
    }
}

