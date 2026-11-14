package org.kcm.bloomery.fawaymc.level.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.kcm.bloomery.fawaymc.level.Global;

public class LevelDataManager {
   private final Global plugin;
   private final File dataFile;
   private final Map<UUID, PlayerLevel> cache = new HashMap();
   private YamlConfiguration dataConfig;
   private int autoSaveTaskId = -1;

   public LevelDataManager(Global plugin) {
      this.plugin = plugin;
      this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
      this.loadAll();
   }

   private void loadAll() {
      try {
         if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
         }

         if (!this.dataFile.exists()) {
            this.dataFile.createNewFile();
         }
      } catch (IOException var7) {
      }

      this.dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
      ConfigurationSection players = this.dataConfig.getConfigurationSection("players");
      if (players != null) {
         for(String key : players.getKeys(false)) {
            try {
               UUID uuid = UUID.fromString(key);
               PlayerLevel lvl = this.loadFromConfig(uuid);
               if (lvl != null) {
                  this.cache.put(uuid, lvl);
               }
            } catch (Exception var6) {
            }
         }

      }
   }

   public PlayerLevel get(UUID uuid) {
      PlayerLevel cached = (PlayerLevel)this.cache.get(uuid);
      if (cached != null) {
         return cached;
      } else {
         PlayerLevel loaded = this.loadFromConfig(uuid);
         if (loaded != null) {
            this.cache.put(uuid, loaded);
            return loaded;
         } else {
            PlayerLevel fresh = new PlayerLevel(uuid);
            this.cache.put(uuid, fresh);
            return fresh;
         }
      }
   }

   private PlayerLevel loadFromConfig(UUID uuid) {
      String path = "players." + String.valueOf(uuid);
      if (!this.dataConfig.contains(path)) {
         return null;
      } else {
         PlayerLevel lvl = new PlayerLevel(uuid);
         lvl.setLevel(Math.max(1, this.dataConfig.getInt(path + ".level", 1)));
         lvl.setExp(Math.max(0L, this.dataConfig.getLong(path + ".exp", 0L)));
         lvl.setTotalExp(Math.max(0L, this.dataConfig.getLong(path + ".total_exp", 0L)));
         lvl.setLastSeen(this.dataConfig.getLong(path + ".last_seen", 0L));
         return lvl;
      }
   }

   public void save(UUID uuid) {
      PlayerLevel lvl = (PlayerLevel)this.cache.get(uuid);
      if (lvl != null) {
         String path = "players." + String.valueOf(uuid);
         this.dataConfig.set(path + ".level", lvl.getLevel());
         this.dataConfig.set(path + ".exp", lvl.getExp());
         this.dataConfig.set(path + ".total_exp", lvl.getTotalExp());
         this.dataConfig.set(path + ".last_seen", System.currentTimeMillis());
         lvl.setLastSeen(System.currentTimeMillis());
      }
   }

   public void saveAll() {
      for(UUID uuid : this.cache.keySet()) {
         this.save(uuid);
      }

      this.flush();
   }

   public void flush() {
      try {
         this.dataConfig.save(this.dataFile);
      } catch (IOException e) {
         this.plugin.getLogger().severe("[FawayLevel] Cannot save playerdata.yml: " + e.getMessage());
      }

   }

   public void startAutoSave() {
      int interval = this.plugin.getConfig().getInt("storage.auto_save_minutes", 5) * 60 * 20;
      if (interval > 0) {
         this.autoSaveTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, this::saveAll, (long)interval, (long)interval).getTaskId();
      }
   }

   public void unload(UUID uuid) {
      this.save(uuid);
      this.flush();
      this.cache.remove(uuid);
   }
}
