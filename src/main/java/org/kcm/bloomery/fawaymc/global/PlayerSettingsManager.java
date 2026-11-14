package org.kcm.bloomery.fawaymc.global;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSettingsManager {

    private final Global plugin;
    private final File settingsFile;
    private FileConfiguration settings;
    private final Map<UUID, Boolean> doubleJumpEnabled = new HashMap<>();

    public PlayerSettingsManager(Global plugin) {
        this.plugin = plugin;
        this.settingsFile = new File(plugin.getDataFolder(), "player_settings.yml");
        load();
    }

    private void load() {
        if (!settingsFile.exists()) {
            try {
                settingsFile.getParentFile().mkdirs();
                settingsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("ไม่สามารถสร้าง player_settings.yml ได้: " + e.getMessage());
            }
        }
        settings = YamlConfiguration.loadConfiguration(settingsFile);
    }

    public void reload() {
        settings = YamlConfiguration.loadConfiguration(settingsFile);
    }

    public boolean isDoubleJumpEnabled(Player player) {
        UUID uuid = player.getUniqueId();
        if (doubleJumpEnabled.containsKey(uuid)) {
            return doubleJumpEnabled.get(uuid);
        }
        boolean value = settings.getBoolean("players." + uuid + ".double_jump", true);
        doubleJumpEnabled.put(uuid, value);
        return value;
    }

    public void setDoubleJumpEnabled(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        doubleJumpEnabled.put(uuid, enabled);
        settings.set("players." + uuid + ".double_jump", enabled);
        save();
    }

    public void toggleDoubleJump(Player player) {
        boolean newValue = !isDoubleJumpEnabled(player);
        setDoubleJumpEnabled(player, newValue);
        plugin.getLogger().info("[DEBUG] " + player.getName() + " double_jump -> " + newValue);
    }

    private void save() {
        try {
            settings.save(settingsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถบันทึก player_settings.yml ได้: " + e.getMessage());
        }
    }

    public void onQuit(Player player) {
        // doubleJumpEnabled.remove(player.getUniqueId());
    }
}