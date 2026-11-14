package org.kcm.bloomery.fawaymc.leaderboard.manager;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.kcm.bloomery.fawaymc.leaderboard.Global;
import org.kcm.bloomery.fawaymc.leaderboard.hologram.HologramConfig;
import org.kcm.bloomery.fawaymc.leaderboard.hologram.LeaderboardHologram;

public class HologramManager {
   private final Global plugin;
   private final Map<String, LeaderboardHologram> holograms = new HashMap();
   private File file;
   private YamlConfiguration config;
   private int realtimeTaskId = -1;

   public HologramManager(Global plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "holograms.yml");
      this.load();
   }

   public void load() {
      if (!this.plugin.getDataFolder().exists()) {
         this.plugin.getDataFolder().mkdirs();
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);
      ConfigurationSection sec = this.config.getConfigurationSection("holograms");
      if (sec != null) {
         for(String id : sec.getKeys(false)) {
            try {
               ConfigurationSection hSec = sec.getConfigurationSection(id);
               if (hSec != null) {
                  HologramConfig cfg = HologramConfig.load(id, hSec);
                  this.holograms.put(id, new LeaderboardHologram(this.plugin, cfg));
               }
            } catch (Exception e) {
               this.plugin.getLogger().warning("[Hologram] Load '" + id + "' failed: " + e.getMessage());
            }
         }

      }
   }

   public void save() {
      if (this.config == null) {
         this.config = new YamlConfiguration();
      }

      this.config.set("holograms", (Object)null);

      for(Map.Entry<String, LeaderboardHologram> e : this.holograms.entrySet()) {
         ((LeaderboardHologram)e.getValue()).getConfig().save(this.config.createSection("holograms." + (String)e.getKey()));
      }

      try {
         this.config.save(this.file);
      } catch (Exception var3) {
      }

   }

   public void save(HologramConfig cfg) {
      if (cfg != null) {
         if (this.config == null) {
            this.config = new YamlConfiguration();
         }

         cfg.save(this.config.createSection("holograms." + cfg.id));

         try {
            this.config.save(this.file);
         } catch (Exception var3) {
         }

      }
   }

   public boolean create(String id, String type, Location loc) {
      if (!this.holograms.containsKey(id) && loc != null && loc.getWorld() != null) {
         HologramConfig cfg = new HologramConfig(id, type, loc.clone());
         LeaderboardHologram holo = new LeaderboardHologram(this.plugin, cfg);
         this.holograms.put(id, holo);
         holo.render();
         this.save(cfg);
         return true;
      } else {
         return false;
      }
   }

   public boolean delete(String id) {
      LeaderboardHologram holo = (LeaderboardHologram)this.holograms.remove(id);
      if (holo == null) {
         return false;
      } else {
         holo.cleanup();
         if (this.config != null) {
            this.config.set("holograms." + id, (Object)null);

            try {
               this.config.save(this.file);
            } catch (Exception var4) {
            }
         }

         return true;
      }
   }

   public LeaderboardHologram get(String id) {
      return (LeaderboardHologram)this.holograms.get(id);
   }

   public Set<String> getIds() {
      return this.holograms.keySet();
   }

   public Collection<LeaderboardHologram> getAll() {
      return this.holograms.values();
   }

   public void render(String id) {
      LeaderboardHologram h = (LeaderboardHologram)this.holograms.get(id);
      if (h != null) {
         h.render();
      }

   }

   public void renderAll() {
      for(LeaderboardHologram h : this.holograms.values()) {
         h.render();
      }

   }

   public void requestRefresh() {
      for(LeaderboardHologram holo : this.holograms.values()) {
         holo.update();
      }

   }

   public void startRealtimeTask() {
      if (this.realtimeTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.realtimeTaskId);
      }

      this.realtimeTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         for(LeaderboardHologram holo : this.holograms.values()) {
            holo.update();
         }

      }, 1L, 2L).getTaskId();
   }

   public void stop() {
      if (this.realtimeTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.realtimeTaskId);
         this.realtimeTaskId = -1;
      }

      for(LeaderboardHologram h : this.holograms.values()) {
         h.cleanup();
      }

      this.holograms.clear();
      this.save();
   }
}
