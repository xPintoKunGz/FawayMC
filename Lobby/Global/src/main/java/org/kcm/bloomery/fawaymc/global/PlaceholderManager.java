package org.kcm.bloomery.fawaymc.global;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PlaceholderManager {
   public static String setPlaceholders(Player player, String text) {
      if (text != null && !text.isEmpty()) {
         String result = replaceInternal(player, text);
         if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
               result = PlaceholderAPI.setPlaceholders(player, result);
            } catch (Throwable var4) {
            }
         }

         result = ChatColor.translateAlternateColorCodes('&', result);
         return result;
      } else {
         return "";
      }
   }

   private static String replaceInternal(Player player, String text) {
      String playerName = player != null ? player.getName() : "Console";
      String rank = "Member";
      if (player != null && player.hasPermission("global.admin")) {
         rank = "Admin";
      }

      int onlineCount = Bukkit.getOnlinePlayers().size();
      int maxOnline = Bukkit.getMaxPlayers();
      Locale thaiLocale = new Locale("th", "TH");
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", thaiLocale);
      String currentDate = dateFormat.format(new Date());
      SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", thaiLocale);
      String currentTime = timeFormat.format(new Date());
      return text.replace("{player}", playerName).replace("{rank}", rank).replace("{online}", String.valueOf(onlineCount)).replace("{max_online}", String.valueOf(maxOnline)).replace("{date}", currentDate).replace("{time}", currentTime);
   }
}
