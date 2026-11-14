package org.kcm.bloomery.fawaymc.luckyblock;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

public class LuckyblockExpansion extends PlaceholderExpansion {
   private final Global plugin;

   public LuckyblockExpansion(Global plugin) {
      this.plugin = plugin;
   }

   public String getIdentifier() {
      return "luckyblock";
   }

   public String getAuthor() {
      return "FawayMC";
   }

   public String getVersion() {
      return this.plugin.getDescription().getVersion();
   }

   public boolean persist() {
      return true;
   }

   public boolean canRegister() {
      return true;
   }

   public String onRequest(OfflinePlayer offlinePlayer, String params) {
      if (offlinePlayer != null && offlinePlayer.getUniqueId() != null) {
         String p = params.toLowerCase();
         boolean formatted = p.endsWith("_formatted");
         String baseKey = formatted ? p.substring(0, p.length() - "_formatted".length()) : p;
         if (baseKey.equals("total")) {
            long v = (long)this.plugin.getData().getTotalBoxCount(offlinePlayer.getUniqueId());
            return formatted ? this.formatNumber(v) : String.valueOf(v);
         } else if (baseKey.equals("coins")) {
            long v = this.plugin.getData().getCoins(offlinePlayer.getUniqueId());
            return formatted ? this.formatNumber(v) : String.valueOf(v);
         } else if (!baseKey.equals("cosmetic_count") && !baseKey.equals("cosmetics")) {
            if (!baseKey.equals("cosmetics_total") && !baseKey.equals("cosmetic_total")) {
               if (!baseKey.equals("cosmetics_equipped") && !baseKey.equals("cosmetic_equipped")) {
                  if (!baseKey.startsWith("cosmetics_owned_") && !baseKey.startsWith("cosmetic_owned_")) {
                     if (!baseKey.startsWith("cosmetics_total_") && !baseKey.startsWith("cosmetic_total_")) {
                        if (baseKey.startsWith("has_")) {
                           String cosmeticId = params.substring(4);
                           return this.plugin.getData().ownsCosmetic(offlinePlayer.getUniqueId(), cosmeticId) ? "yes" : "no";
                        } else if (baseKey.startsWith("equipped_")) {
                           String typeStr = params.substring(9);

                           try {
                              CosmeticType type = CosmeticType.valueOf(typeStr.toUpperCase());
                              String id = this.plugin.getData().getEquipped(offlinePlayer.getUniqueId(), type);
                              return id != null && !id.isEmpty() ? id : "none";
                           } catch (IllegalArgumentException var13) {
                              return "none";
                           }
                        } else {
                           ConfigurationSection boxes = this.plugin.getConfig().getConfigurationSection("boxes");
                           if (boxes != null) {
                              for(String boxId : boxes.getKeys(false)) {
                                 if (baseKey.equals(boxId.toLowerCase())) {
                                    int v = this.plugin.getData().getBoxCount(offlinePlayer.getUniqueId(), boxId);
                                    return formatted ? this.formatNumber((long)v) : String.valueOf(v);
                                 }
                              }
                           }

                           return "";
                        }
                     } else {
                        String typeStr = baseKey.startsWith("cosmetics_total_") ? params.substring("cosmetics_total_".length()) : params.substring("cosmetic_total_".length());

                        try {
                           CosmeticType type = CosmeticType.valueOf(typeStr.toUpperCase());
                           int total = 0;
                           ConfigurationSection cos = this.plugin.getConfig().getConfigurationSection("cosmetics");
                           if (cos != null) {
                              for(String id : cos.getKeys(false)) {
                                 String idType = this.plugin.getConfig().getString("cosmetics." + id + ".type", "TRAIL");
                                 if (CosmeticType.fromString(idType) == type) {
                                    ++total;
                                 }
                              }
                           }

                           return String.valueOf(total);
                        } catch (IllegalArgumentException var14) {
                           return "0";
                        }
                     }
                  } else {
                     String typeStr = baseKey.startsWith("cosmetics_owned_") ? params.substring("cosmetics_owned_".length()) : params.substring("cosmetic_owned_".length());

                     try {
                        CosmeticType type = CosmeticType.valueOf(typeStr.toUpperCase());
                        int owned = 0;
                        ConfigurationSection cos = this.plugin.getConfig().getConfigurationSection("cosmetics");
                        if (cos != null) {
                           for(String id : cos.getKeys(false)) {
                              String idType = this.plugin.getConfig().getString("cosmetics." + id + ".type", "TRAIL");
                              if (CosmeticType.fromString(idType) == type && this.plugin.getData().ownsCosmetic(offlinePlayer.getUniqueId(), id)) {
                                 ++owned;
                              }
                           }
                        }

                        return String.valueOf(owned);
                     } catch (IllegalArgumentException var15) {
                        return "0";
                     }
                  }
               } else {
                  int eq = 0;

                  for(CosmeticType t : CosmeticType.values()) {
                     String id = this.plugin.getData().getEquipped(offlinePlayer.getUniqueId(), t);
                     if (id != null && !id.isEmpty()) {
                        ++eq;
                     }
                  }

                  return String.valueOf(eq);
               }
            } else {
               ConfigurationSection cos = this.plugin.getConfig().getConfigurationSection("cosmetics");
               return String.valueOf(cos == null ? 0 : cos.getKeys(false).size());
            }
         } else {
            int v = this.plugin.getData().getOwnedCosmetics(offlinePlayer.getUniqueId()).size();
            return String.valueOf(v);
         }
      } else {
         return "";
      }
   }

   private String formatNumber(long n) {
      if (n >= 1000000000L) {
         return String.format("%.1fB", (double)n / (double)1.0E9F);
      } else if (n >= 1000000L) {
         return String.format("%.1fM", (double)n / (double)1000000.0F);
      } else {
         return n >= 1000L ? String.format("%.1fk", (double)n / (double)1000.0F) : String.valueOf(n);
      }
   }
}
