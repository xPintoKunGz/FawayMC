package org.kcm.bloomery.fawaymc.leaderboard.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.leaderboard.Global;
import org.kcm.bloomery.fawaymc.leaderboard.data.LeaderboardEntry;
import org.kcm.bloomery.fawaymc.leaderboard.provider.AdvancementProvider;
import org.kcm.bloomery.fawaymc.leaderboard.provider.CoinsProvider;
import org.kcm.bloomery.fawaymc.leaderboard.provider.DailyProvider;
import org.kcm.bloomery.fawaymc.leaderboard.provider.DataProvider;
import org.kcm.bloomery.fawaymc.leaderboard.provider.LevelProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class LeaderboardManager {

   private final Global plugin;
   private final Map<String, DataProvider> providers = new HashMap<>();
   private final Map<String, Map<UUID, Long>> rawCache = new ConcurrentHashMap<>();
   private final Map<String, List<LeaderboardEntry>> sortedCache = new ConcurrentHashMap<>();
   private final Map<UUID, String> nameCache = new ConcurrentHashMap<>();
   private int refreshTaskId = -1;

   public LeaderboardManager(Global plugin) {
      this.plugin = plugin;
   }

   public void registerProviders() {
      this.providers.clear();
      this.tryRegister("advancement", new AdvancementProvider(this.plugin));
      this.tryRegister("level", new LevelProvider(this.plugin));
      this.tryRegister("coins", new CoinsProvider(this.plugin));
      this.tryRegister("daily", new DailyProvider(this.plugin));
   }

   private void tryRegister(String type, DataProvider provider) {
      if (!provider.isAvailable()) {
         this.plugin.getLogger().info("[Leaderboard] Provider '" + type + "' unavailable");
      } else {
         this.providers.put(type, provider);
         this.plugin.getLogger().info("[Leaderboard] ✓ Registered provider: " + type);
      }
   }

   public void importFromSources() {
      if (this.plugin.getConfig().getBoolean("settings.import_on_startup", true)) {
         for (Map.Entry<String, DataProvider> e : this.providers.entrySet()) {
            String type = e.getKey();

            try {
               Map<UUID, Long> data = e.getValue().importAll();
               if (!data.isEmpty()) {
                  Map<UUID, Long> cache = this.rawCache.computeIfAbsent(type, k -> new ConcurrentHashMap<>());
                  cache.putAll(data);
                  this.plugin.getLogger().info("[Leaderboard] Imported " + data.size() + " entries for " + type);
               }
            } catch (Exception ex) {
               this.plugin.getLogger().warning("[Leaderboard] Import " + type + " failed: " + ex.getMessage());
            }
         }

         this.refreshAll();
      }
   }

   public void startRefreshTask() {
      if (this.refreshTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.refreshTaskId);
      }

      this.refreshTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         for (Player p : Bukkit.getOnlinePlayers()) {
            this.refreshPlayer(p);
         }

         this.refreshAll();
      }, 20L, 20L).getTaskId();
   }

   public void refreshPlayer(Player player) {
      UUID uuid = player.getUniqueId();
      this.nameCache.put(uuid, player.getName());

      for (Map.Entry<String, DataProvider> e : this.providers.entrySet()) {
         String type = e.getKey();

         try {
            long v = e.getValue().getValue(uuid);
            if (v > 0L) {
               this.rawCache.computeIfAbsent(type, k -> new ConcurrentHashMap<>()).put(uuid, v);
            }
         } catch (Exception ignored) {}
      }
   }

   public void refreshAll() {
      int topSize = this.plugin.getConfig().getInt("settings.top_size", 10);
      long minValue = this.plugin.getConfig().getLong("settings.min_value", 1L);

      for (String type : this.providers.keySet()) {
         Map<UUID, Long> data = this.rawCache.get(type);
         if (data != null && !data.isEmpty()) {
            List<LeaderboardEntry> entries = new ArrayList<>();

            for (Map.Entry<UUID, Long> e : data.entrySet()) {
               long v = e.getValue();
               if (v >= minValue) {
                  entries.add(new LeaderboardEntry(e.getKey(),
                          this.nameCache.getOrDefault(e.getKey(), this.resolveName(e.getKey())), v));
               }
            }

            entries.sort(Comparator.comparingLong(LeaderboardEntry::getValue).reversed());
            if (entries.size() > topSize) {
               entries = new ArrayList<>(entries.subList(0, topSize));
            }

            this.sortedCache.put(type, entries);
         } else {
            this.sortedCache.put(type, Collections.emptyList());
         }
      }
   }

   private String resolveName(UUID uuid) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null) {
         this.nameCache.put(uuid, p.getName());
         return p.getName();
      }

      try {
         OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
         String name = op.getName();
         if (name != null && !name.isEmpty()) {
            this.nameCache.put(uuid, name);
            return name;
         }
      } catch (Exception ignored) {}

      String name = uuid.toString().substring(0, 8);
      this.nameCache.put(uuid, name);
      return name;
   }

   public List<LeaderboardEntry> getTop(String type) {
      return this.sortedCache.getOrDefault(type, Collections.emptyList());
   }

   public LeaderboardEntry getEntry(String type, int position) {
      if (position < 1) return null;
      List<LeaderboardEntry> list = this.getTop(type);
      return position > list.size() ? null : list.get(position - 1);
   }

   public int getSize(String type) {
      return this.getTop(type).size();
   }

   public Set<String> getRegisteredTypes() {
      return this.providers.keySet();
   }

   public DataProvider getProvider(String type) {
      return this.providers.get(type);
   }

   public void saveCache() {
      try {
         File file = new File(this.plugin.getDataFolder(), "cache.yml");
         YamlConfiguration cfg = new YamlConfiguration();

         // ★ FIX: ลบ (Map) Cast ออก ให้ใช้ Generic Type
         for (Map.Entry<String, Map<UUID, Long>> e : this.rawCache.entrySet()) {
            String type = e.getKey();
            for (Map.Entry<UUID, Long> v : e.getValue().entrySet()) {
               cfg.set("cache." + type + "." + v.getKey().toString(), v.getValue());
            }
         }

         for (Map.Entry<UUID, String> n : this.nameCache.entrySet()) {
            cfg.set("names." + n.getKey().toString(), n.getValue());
         }

         cfg.save(file);
      } catch (Exception e) {
         this.plugin.getLogger().warning("[Leaderboard] Save cache failed: " + e.getMessage());
      }
   }

   public void loadCache() {
      try {
         File file = new File(this.plugin.getDataFolder(), "cache.yml");
         if (!file.exists()) return;

         YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
         ConfigurationSection cacheSec = cfg.getConfigurationSection("cache");
         if (cacheSec != null) {
            for (String type : cacheSec.getKeys(false)) {
               ConfigurationSection tSec = cacheSec.getConfigurationSection(type);
               if (tSec != null) {
                  Map<UUID, Long> map = this.rawCache.computeIfAbsent(type, k -> new ConcurrentHashMap<>());
                  for (String uuidStr : tSec.getKeys(false)) {
                     try {
                        UUID uuid = UUID.fromString(uuidStr);
                        map.put(uuid, tSec.getLong(uuidStr));
                     } catch (Exception ignored) {}
                  }
               }
            }
         }

         ConfigurationSection namesSec = cfg.getConfigurationSection("names");
         if (namesSec != null) {
            for (String uuidStr : namesSec.getKeys(false)) {
               try {
                  UUID uuid = UUID.fromString(uuidStr);
                  this.nameCache.put(uuid, namesSec.getString(uuidStr, ""));
               } catch (Exception ignored) {}
            }
         }

         this.plugin.getLogger().info("[Leaderboard] Cache loaded");
      } catch (Exception e) {
         this.plugin.getLogger().warning("[Leaderboard] Load cache failed: " + e.getMessage());
      }
   }

   public void stop() {
      if (this.refreshTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.refreshTaskId);
         this.refreshTaskId = -1;
      }

      this.saveCache();
   }

   public String formatLine(String type, int position) {
      LeaderboardEntry entry = this.getEntry(type, position);
      String empty = this.plugin.getConfig().getString("leaderboards." + type + ".empty", "&7No data yet");
      if (entry == null) {
         return ChatColor.translateAlternateColorCodes('&', empty);
      }

      String format = this.plugin.getConfig().getString("leaderboards." + type + ".format", "&6#{position} &e{player} &7- &a{value}");
      return ChatColor.translateAlternateColorCodes('&',
              format.replace("{position}", String.valueOf(position))
                      .replace("{player}", entry.getPlayerName())
                      .replace("{value}", this.formatValue(entry.getValue())));
   }

   private String formatValue(long value) {
      if (value >= 1_000_000L) {
         return String.format("%.1fM", (double) value / 1_000_000.0);
      } else if (value >= 1_000L) {
         return String.format("%.1fk", (double) value / 1_000.0);
      }
      return String.valueOf(value);
   }

   public int getPlayerRank(UUID uuid, String type) {
      List<LeaderboardEntry> list = this.getTop(type);

      for (int i = 0; i < list.size(); ++i) {
         if (list.get(i).getUuid().equals(uuid)) {
            return i + 1;
         }
      }

      Map<UUID, Long> raw = this.rawCache.get(type);
      if (raw != null && raw.containsKey(uuid)) {
         long val = raw.get(uuid);
         if (val < this.plugin.getConfig().getLong("settings.min_value", 1L)) {
            return -1;
         }

         int rank = 1;
         for (long v : raw.values()) {
            if (v > val) {
               ++rank;
            }
         }
         return rank;
      }
      return -1;
   }

   public long getPlayerValue(UUID uuid, String type) {
      Map<UUID, Long> raw = this.rawCache.get(type);
      return raw != null ? raw.getOrDefault(uuid, 0L) : 0L;
   }
}