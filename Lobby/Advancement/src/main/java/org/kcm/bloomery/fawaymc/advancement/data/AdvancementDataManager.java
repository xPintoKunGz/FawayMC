package org.kcm.bloomery.fawaymc.advancement.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.kcm.bloomery.fawaymc.advancement.Global;

public class AdvancementDataManager {
   private final Global plugin;
   private final File file;
   private final Map<UUID, PlayerProgress> cache = new HashMap();
   private YamlConfiguration config;
   private int taskId = -1;

   public AdvancementDataManager(Global plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "playerdata.yml");
      this.load();
   }

   private void load() {
      try {
         if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
         }

         if (!this.file.exists()) {
            this.file.createNewFile();
         }
      } catch (IOException var10) {
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);
      ConfigurationSection players = this.config.getConfigurationSection("players");
      if (players != null) {
         for(String key : players.getKeys(false)) {
            try {
               UUID uuid = UUID.fromString(key);
               PlayerProgress p = new PlayerProgress(uuid);
               p.setPlaytimeMinutes(players.getLong(key + ".playtime", 0L));
               ConfigurationSection prog = players.getConfigurationSection(key + ".progress");
               if (prog != null) {
                  for(String advId : prog.getKeys(false)) {
                     p.setProgress(advId, prog.getInt(advId));
                  }
               }

               ConfigurationSection unl = players.getConfigurationSection(key + ".unlocked");
               if (unl != null) {
                  for(String advId : unl.getKeys(false)) {
                     if (unl.getBoolean(advId)) {
                        p.markUnlocked(advId);
                     }
                  }
               }

               this.cache.put(uuid, p);
            } catch (Exception var11) {
            }
         }

      }
   }

   public PlayerProgress get(UUID uuid) {
      PlayerProgress cached = (PlayerProgress)this.cache.get(uuid);
      if (cached != null) {
         return cached;
      } else {
         ConfigurationSection players = this.config.getConfigurationSection("players");
         String path = "players." + String.valueOf(uuid);
         if (players != null && players.contains(uuid.toString())) {
            PlayerProgress p = new PlayerProgress(uuid);
            p.setPlaytimeMinutes(this.config.getLong(path + ".playtime", 0L));
            ConfigurationSection prog = this.config.getConfigurationSection(path + ".progress");
            if (prog != null) {
               for(String advId : prog.getKeys(false)) {
                  p.setProgress(advId, prog.getInt(advId));
               }
            }

            ConfigurationSection unl = this.config.getConfigurationSection(path + ".unlocked");
            if (unl != null) {
               for(String advId : unl.getKeys(false)) {
                  if (unl.getBoolean(advId)) {
                     p.markUnlocked(advId);
                  }
               }
            }

            this.cache.put(uuid, p);
            return p;
         } else {
            PlayerProgress fresh = new PlayerProgress(uuid);
            this.cache.put(uuid, fresh);
            return fresh;
         }
      }
   }

   public void save(UUID uuid) {
      PlayerProgress p = (PlayerProgress)this.cache.get(uuid);
      if (p != null) {
         String path = "players." + String.valueOf(uuid);
         this.config.set(path + ".playtime", p.getPlaytimeMinutes());
         this.config.set(path + ".progress", (Object)null);
         this.config.set(path + ".unlocked", (Object)null);

         for(Map.Entry<String, Integer> e : p.getProgress().entrySet()) {
            this.config.set(path + ".progress." + (String)e.getKey(), e.getValue());
         }

         for(String advId : p.getUnlocked()) {
            this.config.set(path + ".unlocked." + advId, true);
         }

      }
   }

   public void saveAll() {
      for(UUID uuid : this.cache.keySet()) {
         this.save(uuid);
      }

      try {
         this.config.save(this.file);
      } catch (IOException e) {
         this.plugin.getLogger().severe("Cannot save playerdata.yml: " + e.getMessage());
      }

   }

   public void unload(UUID uuid) {
      this.save(uuid);

      try {
         this.config.save(this.file);
      } catch (IOException var3) {
      }

      this.cache.remove(uuid);
   }

   public void startAutoSave() {
      int minutes = this.plugin.getConfig().getInt("settings.auto_save_minutes", 5);
      if (minutes > 0) {
         long ticks = (long)minutes * 60L * 20L;
         this.taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, this::saveAll, ticks, ticks).getTaskId();
      }
   }

   public int[] cleanupInvalid(Set<String> validIds) {
      int cleanedPlayers = 0;
      int cleanedEntries = 0;

      for(PlayerProgress p : this.cache.values()) {
         boolean changed = false;
         Iterator<String> unlockedIt = p.getUnlocked().iterator();

         while(unlockedIt.hasNext()) {
            String advId = (String)unlockedIt.next();
            if (!validIds.contains(advId)) {
               unlockedIt.remove();
               ++cleanedEntries;
               changed = true;
            }
         }

         Iterator<String> progIt = p.getProgress().keySet().iterator();

         while(progIt.hasNext()) {
            String advId = (String)progIt.next();
            if (!validIds.contains(advId)) {
               progIt.remove();
               ++cleanedEntries;
               changed = true;
            }
         }

         if (changed) {
            ++cleanedPlayers;
         }
      }

      if (cleanedEntries > 0) {
         this.saveAll();
      }

      return new int[]{cleanedPlayers, cleanedEntries};
   }

   public void resetAllPlayers() {
      for(PlayerProgress p : this.cache.values()) {
         p.getUnlocked().clear();
         p.getProgress().clear();
         p.setPlaytimeMinutes(0L);
      }

      this.saveAll();
   }

   public void resetPlayer(UUID uuid) {
      PlayerProgress p = (PlayerProgress)this.cache.get(uuid);
      if (p != null) {
         p.getUnlocked().clear();
         p.getProgress().clear();
         p.setPlaytimeMinutes(0L);
         this.save(uuid);
         this.flush();
      }
   }

   public void flush() {
      try {
         this.config.save(this.file);
      } catch (IOException e) {
         this.plugin.getLogger().severe("Cannot save playerdata.yml: " + e.getMessage());
      }

   }
}
