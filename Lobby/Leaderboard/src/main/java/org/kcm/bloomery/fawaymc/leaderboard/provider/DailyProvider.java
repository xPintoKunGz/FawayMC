package org.kcm.bloomery.fawaymc.leaderboard.provider;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.kcm.bloomery.fawaymc.leaderboard.Global;

public class DailyProvider implements DataProvider {
   private final Global plugin;

   public DailyProvider(Global plugin) {
      this.plugin = plugin;
   }

   public String getId() {
      return "daily";
   }

   public boolean isAvailable() {
      return this.findPlugin() != null;
   }

   public long getValue(UUID uuid) {
      return (Long)this.readYaml().getOrDefault(uuid, 0L);
   }

   public Map<UUID, Long> importAll() {
      return this.readYaml();
   }

   private Map<UUID, Long> readYaml() {
      Map<UUID, Long> map = new HashMap();

      try {
         Plugin d = this.findPlugin();
         if (d == null) {
            return map;
         }

         File[] candidates = new File[]{new File(d.getDataFolder(), "playerdata.yml"), new File(d.getDataFolder(), "streaks.yml")};

         for(File file : candidates) {
            if (file.exists()) {
               YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
               ConfigurationSection players = cfg.getConfigurationSection("players");
               if (players != null) {
                  for(String uuidStr : players.getKeys(false)) {
                     try {
                        UUID uuid = UUID.fromString(uuidStr);
                        int streak = players.getInt(uuidStr + ".streak_days", players.getInt(uuidStr + ".streak", 0));
                        if (streak > 0) {
                           map.put(uuid, (long)streak);
                        }
                     } catch (Exception var14) {
                     }
                  }

                  if (!map.isEmpty()) {
                     break;
                  }
               }
            }
         }
      } catch (Exception e) {
         this.plugin.getLogger().warning("[Leaderboard] Daily readYaml failed: " + e.getMessage());
      }

      return map;
   }

   private Plugin findPlugin() {
      List<String> names = this.plugin.getConfig().getStringList("providers.daily.plugin_names");
      PluginManager pm = Bukkit.getPluginManager();

      for(String n : names) {
         Plugin p = pm.getPlugin(n);
         if (p != null && p.isEnabled()) {
            return p;
         }
      }

      for(Plugin p : pm.getPlugins()) {
         if (p.isEnabled() && p.getClass().getName().startsWith("org.kcm.bloomery.fawaymc.daily.")) {
            return p;
         }
      }

      return null;
   }
}
