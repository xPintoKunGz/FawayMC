package org.kcm.bloomery.fawaymc.level.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.level.Global;
import org.kcm.bloomery.fawaymc.level.data.PlayerLevel;

public class TablistManager {
   private final Global plugin;
   private int taskId = -1;

   public TablistManager(Global plugin) {
      this.plugin = plugin;
   }

   public void start() {
      if (this.plugin.getConfig().getBoolean("display.tab.enabled", true)) {
         int interval = this.plugin.getConfig().getInt("display.tab.update_ticks", 20);
         this.taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for(Player p : Bukkit.getOnlinePlayers()) {
               this.update(p);
            }

         }, 20L, (long)Math.max(1, interval)).getTaskId();
      }
   }

   public void update(Player player) {
      if (this.plugin.getConfig().getBoolean("display.tab.enabled", true)) {
         if (player != null && player.isOnline()) {
            boolean requirePerm = this.plugin.getConfig().getBoolean("display.tab.require_permission", false);
            if (requirePerm && !player.hasPermission("fawaylevel.tab.show")) {
               player.setPlayerListName((String)null);
            } else {
               PlayerLevel pl = this.plugin.getDataManager().get(player.getUniqueId());
               String format = this.plugin.getConfig().getString("display.tab.format", "&8[&eLv.{level}&8] &f{player}");
               String name = ChatColor.translateAlternateColorCodes('&', format.replace("{level}", String.valueOf(pl.getLevel())).replace("{player}", player.getName()));
               if (name.length() > 64) {
                  name = name.substring(0, 64);
               }

               try {
                  player.setPlayerListName(name);
               } catch (Exception var7) {
               }

            }
         }
      }
   }
}
