package org.kcm.bloomery.fawaymc.level.placehdoler;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.level.Global;
import org.kcm.bloomery.fawaymc.level.data.PlayerLevel;

public class LevelExpansion extends PlaceholderExpansion {
   private final Global plugin;

   public LevelExpansion(Global plugin) {
      this.plugin = plugin;
   }

   public String getIdentifier() {
      return "level";
   }

   public String getAuthor() {
      return "KCM Bloomery";
   }

   public String getVersion() {
      return this.plugin.getDescription().getVersion();
   }

   public boolean persist() {
      return true;
   }

   public String onRequest(OfflinePlayer offlinePlayer, String params) {
      if (offlinePlayer == null) {
         return "";
      } else {
         PlayerLevel pl = this.plugin.getDataManager().get(offlinePlayer.getUniqueId());
         if (pl == null) {
            return "";
         } else {
            long required = this.plugin.getLevelManager().getRequiredExp(pl.getLevel());
            if (this.plugin.getLevelManager().isMaxLevel(pl.getLevel())) {
               required = 0L;
            }

            switch (params.toLowerCase()) {
               case "level":
                  return String.valueOf(pl.getLevel());
               case "exp":
                  return String.valueOf(pl.getExp());
               case "required":
                  return String.valueOf(required);
               case "remaining":
                  return String.valueOf(required > 0L ? Math.max(0L, required - pl.getExp()) : 0L);
               case "total_exp":
                  return String.valueOf(pl.getTotalExp());
               case "progress":
                  return String.format("%.1f", required > 0L ? (double)pl.getExp() * (double)100.0F / (double)required : (double)100.0F);
               case "progress_int":
                  return String.valueOf(required > 0L ? (int)Math.round((double)pl.getExp() * (double)100.0F / (double)required) : 100);
               case "max_level":
                  return String.valueOf(this.plugin.getLevelManager().isMaxLevel(pl.getLevel()));
               case "bar":
               case "progress_bar":
                  return this.buildBar(pl.getExp(), required);
               case "player_name":
                  Player p = offlinePlayer.getPlayer();
                  return p != null ? p.getName() : offlinePlayer.getName();
               default:
                  return "";
            }
         }
      }
   }

   private String buildBar(long current, long required) {
      int length = this.plugin.getConfig().getInt("placeholder.bar_length", 20);
      String filledStr = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("placeholder.bar_filled", "&a█"));
      String emptyStr = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("placeholder.bar_empty", "&7░"));
      int filled;
      if (required <= 0L) {
         filled = length;
      } else {
         filled = (int)Math.round((double)current / (double)required * (double)length);
         filled = Math.max(0, Math.min(length, filled));
      }

      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < filled; ++i) {
         sb.append(filledStr);
      }

      for(int i = filled; i < length; ++i) {
         sb.append(emptyStr);
      }

      return sb.toString();
   }
}
