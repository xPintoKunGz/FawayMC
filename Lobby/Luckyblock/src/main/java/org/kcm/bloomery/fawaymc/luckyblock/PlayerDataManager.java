package org.kcm.bloomery.fawaymc.luckyblock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerDataManager {
   private final Global plugin;
   private final File dataFile;
   private FileConfiguration data;

   public PlayerDataManager(Global plugin) {
      this.plugin = plugin;
      this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
      this.load();
   }

   public void load() {
      if (!this.dataFile.exists()) {
         try {
            this.dataFile.getParentFile().mkdirs();
            this.dataFile.createNewFile();
         } catch (IOException e) {
            this.plugin.getLogger().severe("Cannot create playerdata.yml: " + e.getMessage());
         }
      }

      this.data = YamlConfiguration.loadConfiguration(this.dataFile);
   }

   public void save() {
      try {
         this.data.save(this.dataFile);
      } catch (IOException e) {
         this.plugin.getLogger().severe("Cannot save playerdata.yml: " + e.getMessage());
      }

   }

   public int getBoxCount(UUID uuid, String type) {
      return this.data.getInt("players." + String.valueOf(uuid) + ".boxes." + type, 0);
   }

   public void setBoxCount(UUID uuid, String type, int count) {
      this.data.set("players." + String.valueOf(uuid) + ".boxes." + type, Math.max(0, count));
      this.save();
   }

   public int addBox(UUID uuid, String type, int amount) {
      if (amount <= 0) {
         return 0;
      } else {
         long max = this.plugin.getConfig().getLong("vault.max_boxes", 1000L);
         int currentTotal = this.getTotalBoxCount(uuid);
         if ((long)currentTotal >= max) {
            return 0;
         } else {
            int canAdd = (int)Math.min((long)amount, max - (long)currentTotal);
            this.setBoxCount(uuid, type, this.getBoxCount(uuid, type) + canAdd);
            return canAdd;
         }
      }
   }

   public boolean removeBox(UUID uuid, String type, int amount) {
      int cur = this.getBoxCount(uuid, type);
      if (cur < amount) {
         return false;
      } else {
         this.setBoxCount(uuid, type, cur - amount);
         return true;
      }
   }

   public void clearAllBoxes(UUID uuid) {
      this.data.set("players." + String.valueOf(uuid) + ".boxes", (Object)null);
      this.save();
   }

   public void clearBoxType(UUID uuid, String type) {
      this.data.set("players." + String.valueOf(uuid) + ".boxes." + type, (Object)null);
      this.save();
   }

   public int removeFromAllTypes(UUID uuid, int amount) {
      Map<String, Integer> counts = this.getAllBoxCounts(uuid);
      int removed = 0;

      for(Map.Entry<String, Integer> e : counts.entrySet()) {
         if (removed >= amount) {
            break;
         }

         int take = Math.min((Integer)e.getValue(), amount - removed);
         if (take > 0) {
            this.removeBox(uuid, (String)e.getKey(), take);
            removed += take;
         }
      }

      return removed;
   }

   public int getTotalBoxCount(UUID uuid) {
      int total = 0;
      ConfigurationSection sec = this.data.getConfigurationSection("players." + String.valueOf(uuid) + ".boxes");
      if (sec != null) {
         for(String k : sec.getKeys(false)) {
            total += sec.getInt(k, 0);
         }
      }

      return total;
   }

   public Map<String, Integer> getAllBoxCounts(UUID uuid) {
      Map<String, Integer> map = new HashMap();
      ConfigurationSection sec = this.data.getConfigurationSection("players." + String.valueOf(uuid) + ".boxes");
      if (sec != null) {
         for(String k : sec.getKeys(false)) {
            map.put(k, sec.getInt(k, 0));
         }
      }

      return map;
   }

   public boolean ownsCosmetic(UUID uuid, String id) {
      return this.data.getStringList("players." + String.valueOf(uuid) + ".owned").contains(id);
   }

   public void giveCosmetic(UUID uuid, String id) {
      List<String> list = new ArrayList(this.data.getStringList("players." + String.valueOf(uuid) + ".owned"));
      if (!list.contains(id)) {
         list.add(id);
         this.data.set("players." + String.valueOf(uuid) + ".owned", list);
         this.save();
      }

   }

   public boolean removeCosmetic(UUID uuid, String cosmeticId) {
      List<String> list = new ArrayList(this.data.getStringList("players." + String.valueOf(uuid) + ".owned"));
      if (!list.contains(cosmeticId)) {
         return false;
      } else {
         list.remove(cosmeticId);
         this.data.set("players." + String.valueOf(uuid) + ".owned", list);

         for(CosmeticType type : CosmeticType.values()) {
            String equipped = this.getEquipped(uuid, type);
            if (cosmeticId.equals(equipped)) {
               this.setEquipped(uuid, type, (String)null);
            }
         }

         this.save();
         return true;
      }
   }

   public int clearAllCosmetics(UUID uuid) {
      List<String> list = new ArrayList(this.data.getStringList("players." + String.valueOf(uuid) + ".owned"));
      int removed = list.size();
      this.data.set("players." + String.valueOf(uuid) + ".owned", (Object)null);

      for(CosmeticType type : CosmeticType.values()) {
         this.setEquipped(uuid, type, (String)null);
      }

      this.save();
      return removed;
   }

   public List<String> getOwnedCosmetics(UUID uuid) {
      return this.data.getStringList("players." + String.valueOf(uuid) + ".owned");
   }

   public String getEquipped(UUID uuid, CosmeticType type) {
      FileConfiguration var10000 = this.data;
      String var10001 = String.valueOf(uuid);
      return var10000.getString("players." + var10001 + ".equipped." + type.name().toLowerCase());
   }

   public void setEquipped(UUID uuid, CosmeticType type, String id) {
      if (id != null && !id.isEmpty()) {
         this.data.set("players." + String.valueOf(uuid) + ".equipped." + type.name().toLowerCase(), id);
      } else {
         this.data.set("players." + String.valueOf(uuid) + ".equipped." + type.name().toLowerCase(), (Object)null);
      }

      this.save();
   }

   public long getCoins(UUID uuid) {
      return this.data.getLong("players." + String.valueOf(uuid) + ".coins", 0L);
   }

   public long addCoins(UUID uuid, long amount) {
      long max = this.plugin.getConfig().getLong("economy.max_coins", 100000L);
      long current = this.getCoins(uuid);
      long newVal = Math.max(0L, Math.min(max, current + amount));
      long actual = newVal - current;
      this.data.set("players." + String.valueOf(uuid) + ".coins", newVal);
      this.save();
      return actual;
   }

   public void setCoins(UUID uuid, long amount) {
      long max = this.plugin.getConfig().getLong("economy.max_coins", 100000L);
      long newVal = Math.max(0L, Math.min(max, amount));
      this.data.set("players." + String.valueOf(uuid) + ".coins", newVal);
      this.save();
   }
}
