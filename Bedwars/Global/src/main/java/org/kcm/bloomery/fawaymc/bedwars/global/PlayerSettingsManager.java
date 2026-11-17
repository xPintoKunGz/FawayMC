package org.kcm.bloomery.fawaymc.bedwars.global;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class PlayerSettingsManager {
   private final Global plugin;
   private final File settingsFile;
   private FileConfiguration settings;
   private final Map<UUID, Boolean> doubleJumpEnabled = new HashMap();

   public PlayerSettingsManager(Global plugin) {
      this.plugin = plugin;
      this.settingsFile = new File(plugin.getDataFolder(), "player_settings.yml");
      this.load();
   }

   private void load() {
      if (!this.settingsFile.exists()) {
         try {
            this.settingsFile.getParentFile().mkdirs();
            this.settingsFile.createNewFile();
         } catch (IOException e) {
            this.plugin.getLogger().severe("ไม่สามารถสร้าง player_settings.yml ได้: " + e.getMessage());
         }
      }

      this.settings = YamlConfiguration.loadConfiguration(this.settingsFile);
   }

   public void reload() {
      this.settings = YamlConfiguration.loadConfiguration(this.settingsFile);
   }

   public boolean isDoubleJumpEnabled(Player player) {
      UUID uuid = player.getUniqueId();
      if (this.doubleJumpEnabled.containsKey(uuid)) {
         return (Boolean)this.doubleJumpEnabled.get(uuid);
      } else {
         boolean value = this.settings.getBoolean("players." + String.valueOf(uuid) + ".double_jump", true);
         this.doubleJumpEnabled.put(uuid, value);
         return value;
      }
   }

   public void setDoubleJumpEnabled(Player player, boolean enabled) {
      UUID uuid = player.getUniqueId();
      this.doubleJumpEnabled.put(uuid, enabled);
      this.settings.set("players." + String.valueOf(uuid) + ".double_jump", enabled);
      this.save();
   }

   public void toggleDoubleJump(Player player) {
      boolean newValue = !this.isDoubleJumpEnabled(player);
      this.setDoubleJumpEnabled(player, newValue);
      Logger var10000 = this.plugin.getLogger();
      String var10001 = player.getName();
      var10000.info("[DEBUG] " + var10001 + " double_jump -> " + newValue);
   }

   private void save() {
      try {
         this.settings.save(this.settingsFile);
      } catch (IOException e) {
         this.plugin.getLogger().severe("ไม่สามารถบันทึก player_settings.yml ได้: " + e.getMessage());
      }

   }

   public void onQuit(Player player) {
   }
}
