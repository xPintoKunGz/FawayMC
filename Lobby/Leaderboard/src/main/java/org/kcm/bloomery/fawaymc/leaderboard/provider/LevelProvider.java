package org.kcm.bloomery.fawaymc.leaderboard.provider;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.kcm.bloomery.fawaymc.leaderboard.Global;

public class LevelProvider implements DataProvider {
   private final Global plugin;

   public LevelProvider(Global plugin) {
      this.plugin = plugin;
   }

   public String getId() {
      return "level";
   }

   public boolean isAvailable() {
      return this.findPlugin() != null;
   }

   public long getValue(UUID uuid) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null) {
         long v = this.readOnline(p);
         if (v >= 0L) {
            return v;
         }
      }

      return (Long)this.readYaml().getOrDefault(uuid, 0L);
   }

   private long readOnline(Player player) {
      try {
         Plugin fl = this.findPlugin();
         if (fl == null) {
            return -1L;
         }

         Object mgr = fl.getClass().getMethod("getDataManager").invoke(fl);
         if (mgr == null) {
            return -1L;
         }

         Object prog = mgr.getClass().getMethod("get", UUID.class).invoke(mgr, player.getUniqueId());
         if (prog == null) {
            return -1L;
         }

         Object lvl = prog.getClass().getMethod("getLevel").invoke(prog);
         if (lvl instanceof Number) {
            return ((Number)lvl).longValue();
         }
      } catch (Exception var6) {
      }

      return -1L;
   }

   public Map<UUID, Long> importAll() {
      return this.readYaml();
   }

   private Map<UUID, Long> readYaml() {
      Map<UUID, Long> map = new HashMap();

      try {
         Plugin fl = this.findPlugin();
         if (fl == null) {
            return map;
         }

         File file = new File(fl.getDataFolder(), "playerdata.yml");
         if (!file.exists()) {
            return map;
         }

         YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
         ConfigurationSection players = cfg.getConfigurationSection("players");
         if (players == null) {
            return map;
         }

         for(String uuidStr : players.getKeys(false)) {
            try {
               UUID uuid = UUID.fromString(uuidStr);
               int lvl = players.getInt(uuidStr + ".level", 0);
               if (lvl > 0) {
                  map.put(uuid, (long)lvl);
               }
            } catch (Exception var10) {
            }
         }
      } catch (Exception e) {
         this.plugin.getLogger().warning("[Leaderboard] Level readYaml failed: " + e.getMessage());
      }

      return map;
   }

   private Plugin findPlugin() {
      List<String> names = this.plugin.getConfig().getStringList("providers.level.plugin_names");
      PluginManager pm = Bukkit.getPluginManager();

      for(String n : names) {
         Plugin p = pm.getPlugin(n);
         if (p != null && p.isEnabled()) {
            return p;
         }
      }

      for(Plugin p : pm.getPlugins()) {
         if (p.isEnabled() && p.getClass().getName().startsWith("org.kcm.bloomery.fawaymc.level.")) {
            return p;
         }
      }

      return null;
   }
}
