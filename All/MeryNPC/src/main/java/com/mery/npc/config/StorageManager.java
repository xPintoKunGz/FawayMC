package com.mery.npc.config;

import com.mery.npc.MeryNPC;
import com.mery.npc.model.NPCData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class StorageManager {

    private final MeryNPC plugin;
    private final File file;
    private FileConfiguration config;

    public StorageManager(MeryNPC plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public void loadNPCs() {
        if (!file.exists()) {
            return;
        }

        config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("npcs");
        if (section == null) return;

        int loaded = 0;
        for (String key : section.getKeys(false)) {
            try {
                ConfigurationSection npcSec = section.getConfigurationSection(key);
                if (npcSec != null) {
                    Map<String, Object> values = npcSec.getValues(false);
                    NPCData data = NPCData.deserialize(values);
                    plugin.getNpcManager().registerNPC(data);
                    plugin.getNpcManager().spawnNPC(data);
                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load NPC with key: " + key, e);
            }
        }

        plugin.getLogger().info("Successfully loaded " + loaded + " MeryNPC personas.");
    }

    public void saveNPCs() {
        if (config == null) {
            config = new YamlConfiguration();
        }

        config.set("npcs", null); // clear existing
        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            config.set("npcs." + data.getId(), data.serialize());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save npcs.yml!", e);
        }
    }
}

