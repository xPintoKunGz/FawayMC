package org.kcm.bloomery.fawaymc.leaderboard.provider;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface DataProvider {
   String getId();

   long getValue(UUID var1);

   Map<UUID, Long> importAll();

   default String getPlayerName(UUID uuid) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null) {
         return p.getName();
      } else {
         try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName();
            if (name != null && !name.isEmpty()) {
               return name;
            }
         } catch (Exception var5) {
         }

         return uuid.toString().substring(0, 8);
      }
   }

   boolean isAvailable();
}
