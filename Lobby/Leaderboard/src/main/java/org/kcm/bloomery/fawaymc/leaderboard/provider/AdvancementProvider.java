package org.kcm.bloomery.fawaymc.leaderboard.provider;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.kcm.bloomery.fawaymc.leaderboard.Global;

public class AdvancementProvider implements DataProvider {
   private final Global plugin;

   public AdvancementProvider(Global plugin) {
      this.plugin = plugin;
   }

   public String getId() {
      return "advancement";
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
         Plugin adv = this.findPlugin();
         if (adv == null) {
            return -1L;
         }

         Object mgr = adv.getClass().getMethod("getDataManager").invoke(adv);
         if (mgr == null) {
            return -1L;
         }

         Object progress = mgr.getClass().getMethod("get", UUID.class).invoke(mgr, player.getUniqueId());
         if (progress == null) {
            return -1L;
         }

         Object unlockedObj = progress.getClass().getMethod("getUnlocked").invoke(progress);
         if (unlockedObj instanceof Set) {
            return (long)((Set)unlockedObj).size();
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
         Plugin adv = this.findPlugin();
         if (adv == null) {
            return map;
         }

         File file = new File(adv.getDataFolder(), "playerdata.yml");
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
               ConfigurationSection unlocked = players.getConfigurationSection(uuidStr + ".unlocked");
               int count = unlocked != null ? unlocked.getKeys(false).size() : 0;
               if (count > 0) {
                  map.put(uuid, (long)count);
               }
            } catch (Exception var11) {
            }
         }
      } catch (Exception e) {
         this.plugin.getLogger().warning("[Leaderboard] Advancement readYaml failed: " + e.getMessage());
      }

      return map;
   }

   private Plugin findPlugin() {
      List<String> names = this.plugin.getConfig().getStringList("providers.advancement.plugin_names");
      PluginManager pm = Bukkit.getPluginManager();

      for(String n : names) {
         Plugin p = pm.getPlugin(n);
         if (p != null && p.isEnabled()) {
            return p;
         }
      }

      for(Plugin p : pm.getPlugins()) {
         if (p.isEnabled() && p.getClass().getName().startsWith("org.kcm.bloomery.fawaymc.advancement.")) {
            return p;
         }
      }

      return null;
   }
}
