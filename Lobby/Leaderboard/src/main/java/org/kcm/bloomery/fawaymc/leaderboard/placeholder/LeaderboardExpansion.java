package org.kcm.bloomery.fawaymc.leaderboard.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.kcm.bloomery.fawaymc.leaderboard.Global;
import org.kcm.bloomery.fawaymc.leaderboard.data.LeaderboardEntry;

public class LeaderboardExpansion extends PlaceholderExpansion {
   private final Global plugin;

   public LeaderboardExpansion(Global plugin) {
      this.plugin = plugin;
   }

   public String getIdentifier() {
      return "leaderboard";
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
      if (params != null && !params.isEmpty()) {
         String[] parts = params.toLowerCase().split("_");
         if (parts.length < 2) {
            return "";
         } else {
            String type = parts[0];
            if (parts[1].equals("size")) {
               return String.valueOf(this.plugin.getLeaderboardManager().getSize(type));
            } else if (parts[1].equals("top")) {
               return this.plugin.getLeaderboardManager().formatLine(type, 1);
            } else if (parts.length >= 2) {
               int position;
               try {
                  position = Integer.parseInt(parts[1]);
               } catch (NumberFormatException var9) {
                  return "";
               }

               LeaderboardEntry entry = this.plugin.getLeaderboardManager().getEntry(type, position);
               if (entry == null) {
                  if (parts.length >= 3 && parts[2].equals("name")) {
                     return "---";
                  } else if (parts.length >= 3 && parts[2].equals("value")) {
                     return "0";
                  } else {
                     return parts.length >= 3 && parts[2].equals("prefix") ? this.color("&7#" + position) : this.color(this.plugin.getConfig().getString("leaderboards." + type + ".empty", "&7No data yet"));
                  }
               } else {
                  if (parts.length >= 3) {
                     switch (parts[2]) {
                        case "name" -> {
                           return entry.getPlayerName();
                        }
                        case "value" -> {
                           return String.valueOf(entry.getValue());
                        }
                        case "formatted" -> {
                           return this.formatValue(entry.getValue());
                        }
                        case "prefix" -> {
                           return this.color("&6#" + position);
                        }
                        case "uuid" -> {
                           return entry.getUuid().toString();
                        }
                     }
                  }

                  return this.plugin.getLeaderboardManager().formatLine(type, position);
               }
            } else {
               return "";
            }
         }
      } else {
         return "";
      }
   }

   private String formatValue(long value) {
      if (value >= 1000000L) {
         return String.format("%.1fM", (double)value / (double)1000000.0F);
      } else {
         return value >= 1000L ? String.format("%.1fk", (double)value / (double)1000.0F) : String.valueOf(value);
      }
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }
}
