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

public class CoinsProvider implements DataProvider {
   private final Global plugin;

   public CoinsProvider(Global plugin) {
      this.plugin = plugin;
   }

   public String getId() {
      return "coins";
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
         Plugin lb = this.findPlugin();
         if (lb == null) {
            return -1L;
         }

         Object mgr = lb.getClass().getMethod("getData").invoke(lb);
         if (mgr == null) {
            return -1L;
         }

         Object r = mgr.getClass().getMethod("getCoins", UUID.class).invoke(mgr, player.getUniqueId());
         if (r instanceof Number) {
            return ((Number)r).longValue();
         }
      } catch (Exception var5) {
      }

      return -1L;
   }

   public Map<UUID, Long> importAll() {
      return this.readYaml();
   }

   private Map<UUID, Long> readYaml() {
      Map<UUID, Long> map = new HashMap();

      try {
         Plugin lb = this.findPlugin();
         if (lb == null) {
            return map;
         }

         File file = new File(lb.getDataFolder(), "playerdata.yml");
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
               long coins = players.getLong(uuidStr + ".coins", 0L);
               if (coins > 0L) {
                  map.put(uuid, coins);
               }
            } catch (Exception var11) {
            }
         }
      } catch (Exception e) {
         this.plugin.getLogger().warning("[Leaderboard] Coins readYaml failed: " + e.getMessage());
      }

      return map;
   }

   private Plugin findPlugin() {
      List<String> names = this.plugin.getConfig().getStringList("providers.coins.plugin_names");
      PluginManager pm = Bukkit.getPluginManager();

      for(String n : names) {
         Plugin p = pm.getPlugin(n);
         if (p != null && p.isEnabled()) {
            return p;
         }
      }

      for(Plugin p : pm.getPlugins()) {
         if (p.isEnabled() && p.getClass().getName().startsWith("org.kcm.bloomery.fawaymc.luckyblock.")) {
            return p;
         }
      }

      return null;
   }
}
