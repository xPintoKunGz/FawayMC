package com.mery.npc.config;

import com.mery.npc.MeryNPC;
import com.mery.npc.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final MeryNPC plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    private String prefix;
    private boolean soundsEnabled;
    private double lookDistance;
    private double defaultHologramOffset;
    private int autoSaveMinutes;

    public ConfigManager(MeryNPC plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        // Save default config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Save default messages.yml
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Check defaults from jar if missing
        InputStream defMsgStream = plugin.getResource("messages.yml");
        if (defMsgStream != null) {
            YamlConfiguration defMsgConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defMsgStream, StandardCharsets.UTF_8));
            messages.setDefaults(defMsgConfig);
        }

        // Cache common settings
        this.prefix = ColorUtil.colorize(messages.getString("prefix", "&#00F2FE◆ &#4FACFEMery&#00F2FENPC &8» &7"));
        this.soundsEnabled = config.getBoolean("settings.enable-sounds", true);
        this.lookDistance = config.getDouble("settings.defaults.look-distance", 6.0);
        this.defaultHologramOffset = config.getDouble("settings.defaults.hologram-offset", 0.35);
        this.autoSaveMinutes = config.getInt("settings.auto-save-interval-minutes", 5);
    }

    public String getMessage(String key) {
        String msg = messages.getString("messages." + key, "");
        if (msg == null || msg.isEmpty()) {
            return "";
        }
        return ColorUtil.colorize(msg.replace("%prefix%", prefix));
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                msg = msg.replace(replacements[i], replacements[i + 1]);
            }
        }
        return msg;
    }

    public String getPrefix() { return prefix; }
    public boolean isSoundsEnabled() { return soundsEnabled; }
    public double getLookDistance() { return lookDistance; }
    public double getDefaultHologramOffset() { return defaultHologramOffset; }
    public int getAutoSaveMinutes() { return autoSaveMinutes; }
    public FileConfiguration getConfig() { return config; }
}

